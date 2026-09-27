package com.rakesh.urlshortener;

import com.rakesh.urlshortener.link.LinkService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(TestTime.class)
class LifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TestTime.MutableClock clock;
    @Autowired LinkService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void resetTime() { clock.set(Instant.parse("2030-01-01T12:00:00Z")); }

    @Test void expiryIsEffectiveAtExactBoundaryAndDoesNotCount() throws Exception {
        String code = create(Map.of("url", "https://example.com", "expiresAt", "2030-01-01T12:01:00Z"));
        clock.set(Instant.parse("2030-01-01T12:01:00Z"));
        mvc.perform(get("/s/" + code)).andExpect(status().isGone());
        mvc.perform(get("/api/links/" + code).header("Authorization", CoreIntegrationTest.AUTH))
                .andExpect(jsonPath("$.status").value("EXPIRED")).andExpect(jsonPath("$.totalClicks").value(0));
    }

    @Test void expiryMustBeFutureAndWithinOneYear() throws Exception {
        for (String expiry : new String[]{"2030-01-01T12:00:00Z", "2032-01-01T00:00:00Z"}) {
            mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("url", "https://example.com", "expiresAt", expiry))))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Test void disableIsIdempotentAndKeepsAnUnusableTombstone() throws Exception {
        String code = create(Map.of("url", "https://example.com"));
        for (int i = 0; i < 2; i++) mvc.perform(delete("/api/links/" + code).header("Authorization", CoreIntegrationTest.AUTH))
                .andExpect(status().isNoContent());
        mvc.perform(get("/s/" + code)).andExpect(status().isGone());
        mvc.perform(get("/api/links/" + code).header("Authorization", CoreIntegrationTest.AUTH))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test void concurrentRedirectsHaveNoLostCountsAndHeadIsNotAClick() throws Exception {
        String code = create(Map.of("url", "https://example.com"));
        var pool = Executors.newFixedThreadPool(8);
        try {
            var work = new ArrayList<Callable<String>>();
            for (int i = 0; i < 40; i++) work.add(() -> service.resolve(code, true));
            for (var result : pool.invokeAll(work)) assertThat(result.get()).isEqualTo("https://example.com");
        } finally { pool.shutdownNow(); }
        mvc.perform(head("/s/" + code)).andExpect(status().isFound()).andExpect(content().string(""));
        mvc.perform(get("/api/links/" + code + "/stats").header("Authorization", CoreIntegrationTest.AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(40))
                .andExpect(jsonPath("$.daily.length()").value(30)).andExpect(jsonPath("$.daily[29].clicks").value(40));
    }

    @Test void dailyBucketsUseUtcAndContainZeros() throws Exception {
        clock.set(Instant.parse("2030-01-01T23:59:59Z"));
        String code = create(Map.of("url", "https://example.com"));
        service.resolve(code, true);
        clock.set(Instant.parse("2030-01-02T00:00:00Z"));
        service.resolve(code, true);
        mvc.perform(get("/api/links/" + code + "/stats").header("Authorization", CoreIntegrationTest.AUTH))
                .andExpect(jsonPath("$.daily[0].clicks").value(0))
                .andExpect(jsonPath("$.daily[28].date").value("2030-01-01"))
                .andExpect(jsonPath("$.daily[28].clicks").value(1))
                .andExpect(jsonPath("$.daily[29].clicks").value(1));
    }

    @Test void failedDailyWriteRollsBackLifetimeCounter() throws Exception {
        String code = create(Map.of("url", "https://example.com"));
        jdbc.execute("ALTER TABLE daily_clicks ADD CONSTRAINT reject_test_click CHECK (code <> '" + code + "')");
        try {
            mvc.perform(get("/s/" + code)).andExpect(status().isServiceUnavailable());
            assertThat(service.get(code).totalClicks()).isZero();
        } finally { jdbc.execute("ALTER TABLE daily_clicks DROP CONSTRAINT reject_test_click"); }
    }

    private String create(Map<String, Object> payload) throws Exception {
        var result = mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH)
                .contentType("application/json").content(json.writeValueAsString(payload)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("code").asText();
    }
}
