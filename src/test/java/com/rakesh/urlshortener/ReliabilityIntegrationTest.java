package com.rakesh.urlshortener;

import com.rakesh.urlshortener.api.CreateLinkRequest;
import com.rakesh.urlshortener.link.LinkService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
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
class ReliabilityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LinkService service;
    @Autowired TestTime.MutableClock clock;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void reset() { clock.set(Instant.parse("2030-01-01T12:00:00Z")); }

    @Test void sameKeyReplaysAndChangedPayloadConflicts() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = "{\"url\":\"https://example.com/?a=1\"}";
        var first = mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH)
                .header("Idempotency-Key", key).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn();
        String code = json.readTree(first.getResponse().getContentAsString()).get("code").asText();
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH)
                .header("Idempotency-Key", key).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH)
                .header("Idempotency-Key", key).contentType("application/json").content("{\"url\":\"https://example.org\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM idempotency_keys WHERE code = ?", Integer.class, code)).isEqualTo(1);
    }

    @Test void successfulCreationCanBeReplayedAfterExpiry() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = json.writeValueAsString(Map.of("url", "https://example.com", "expiresAt", "2030-01-01T12:01:00Z"));
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).header("Idempotency-Key", key)
                .contentType("application/json").content(body)).andExpect(status().isCreated());
        clock.set(Instant.parse("2030-01-01T12:01:00Z"));
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).header("Idempotency-Key", key)
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test void simultaneousRetriesCreateExactlyOneResource() throws Exception {
        var request = new CreateLinkRequest("https://example.com/concurrent", null, null, null);
        String key = UUID.randomUUID().toString();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<Callable<LinkService.CreateResult>>();
            for (int i = 0; i < 20; i++) tasks.add(() -> service.create(request, key));
            var results = new ArrayList<LinkService.CreateResult>();
            for (var future : pool.invokeAll(tasks)) results.add(future.get());
            assertThat(results.stream().map(r -> r.link().code()).distinct().count()).isEqualTo(1);
            assertThat(results.stream().filter(r -> !r.replayed()).count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void rejectsOversizedBodyAndInvalidIdempotencyKey() throws Exception {
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).contentType("application/json")
                .content("{\"url\":\"" + "a".repeat(9000) + "\"}"))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).header("Idempotency-Key", "bad key")
                .contentType("application/json").content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void errorsIncludeServerGeneratedCorrelationId() throws Exception {
        var result = mvc.perform(get("/api/links").header("X-Request-ID", "untrusted-client-value"))
                .andExpect(status().isUnauthorized()).andExpect(header().exists("X-Request-ID")).andReturn();
        String id = result.getResponse().getHeader("X-Request-ID");
        assertThat(id).isNotEqualTo("untrusted-client-value");
        assertThat(json.readTree(result.getResponse().getContentAsString()).get("requestId").asText()).isEqualTo(id);
    }

    @Test void legacyNumericIpSpellingsAreRejected() throws Exception {
        for (String url : new String[]{"http://0177.0.0.1", "http://0x7f.0.0.1", "http://127.1"}) {
            mvc.perform(post("/api/links").header("Authorization", CoreIntegrationTest.AUTH).contentType("application/json")
                    .content(json.writeValueAsString(Map.of("url", url))))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
