package com.rakesh.urlshortener;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreIntegrationTest {
    static final String AUTH = "Bearer test-only-token-with-at-least-32-characters";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void createRedirectAndReadPreservesQueryAndFragment() throws Exception {
        String url = "https://example.com/docs?id=42&utm_source=mail#install";
        JsonNode link = create(Map.of("url", url, "title", "Setup guide"));
        String code = link.get("code").asText();
        assertThat(code).matches("[A-Za-z0-9_-]{12}");
        assertThat(link.get("shortUrl").asText()).isEqualTo("https://sho.rt/s/" + code);
        mvc.perform(get("/s/" + code)).andExpect(status().isFound())
                .andExpect(header().string("Location", url))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/links/" + code).header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(1));
    }

    @Test
    void aliasConflictsNeverOverwriteExistingDestination() throws Exception {
        String alias = "docs-" + UUID.randomUUID().toString().substring(0, 8);
        create(Map.of("url", "https://example.com", "customAlias", alias));
        mvc.perform(post("/api/links").header("Authorization", AUTH).contentType("application/json")
                .content(json.writeValueAsString(Map.of("url", "https://example.org", "customAlias", alias))))
                .andExpect(status().isConflict());
        mvc.perform(get("/s/" + alias)).andExpect(header().string("Location", "https://example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///etc/passwd", "https://a:b@example.com",
            "http://localhost", "http://127.0.0.1", "http://10.1.2.3", "http://[::1]",
            "http://service.local", "http://internal", "https://sho.rt/s/abcd", "https:example.com",
            "https://example.com/%0d%0aLocation:x"})
    void rejectsUnsafeDestinations(String url) throws Exception {
        mvc.perform(post("/api/links").header("Authorization", AUTH).contentType("application/json")
                .content(json.writeValueAsString(Map.of("url", url))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void managementRequiresTokenAndDoesNotExposeDetails() throws Exception {
        for (String path : new String[]{"/api/links", "/api/links/abcd", "/api/links/abcd/stats"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("unauthorized"));
        }
        mvc.perform(post("/api/links").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedUnknownAndOversizedInputs() throws Exception {
        mvc.perform(post("/api/links").header("Authorization", AUTH).contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_json"));
        mvc.perform(post("/api/links").header("Authorization", AUTH).contentType("application/json")
                .content("{\"url\":\"https://example.com\",\"surprise\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/links?size=101").header("Authorization", AUTH))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/s/missing")).andExpect(status().isNotFound());
    }

    @Test
    void listHasBoundedStablePagination() throws Exception {
        create(Map.of("url", "https://example.com"));
        mvc.perform(get("/api/links?size=1&page=0").header("Authorization", AUTH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").isNumber());
    }

    JsonNode create(Map<String, Object> body) throws Exception {
        var result = mvc.perform(post("/api/links").header("Authorization", AUTH)
                .contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
