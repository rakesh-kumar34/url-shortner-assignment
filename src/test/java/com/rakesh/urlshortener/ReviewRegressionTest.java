package com.rakesh.urlshortener;

import com.rakesh.urlshortener.api.CreateUrlRequest;
import com.rakesh.urlshortener.config.AppProperties;
import com.rakesh.urlshortener.service.ShortCodeGenerator;
import com.rakesh.urlshortener.service.UrlService;
import com.rakesh.urlshortener.service.UrlPolicy;
import com.rakesh.urlshortener.persistence.DailyClickRepository;
import com.rakesh.urlshortener.persistence.IdempotencyRepository;
import com.rakesh.urlshortener.persistence.UrlRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ReviewRegressionTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UrlRepository urls;
    @Autowired UrlPolicy policy;
    @Autowired AppProperties config;
    @Autowired Clock clock;
    @Autowired TransactionTemplate transactions;
    @Autowired DailyClickRepository daily;
    @Autowired IdempotencyRepository keys;

    @Test void encodedDestinationMustFitStorageWithOrWithoutAlias() throws Exception {
        String url = "https://example.com/" + "é".repeat(700);
        for (var payload : new Map[]{Map.of("url", url), Map.of("url", url, "customAlias", "unicode-alias")}) {
            mvc.perform(post("/api/urls").header("Authorization", CoreIntegrationTest.AUTH).contentType("application/json")
                    .content(json.writeValueAsString(payload))).andExpect(status().isUnprocessableEntity());
        }
    }

    @Test void expiryFinerThanDatabasePrecisionIsRejected() throws Exception {
        Instant expiry = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(1, ChronoUnit.DAYS).plusNanos(1);
        mvc.perform(post("/api/urls").header("Authorization", CoreIntegrationTest.AUTH).contentType("application/json")
                .content(json.writeValueAsString(Map.of("url", "https://example.com", "expiresAt", expiry.toString()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void randomCollisionsRetryAndExhaustionIsBounded() {
        String first = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String second = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var generator = new ShortCodeGenerator() {
            int calls;
            @Override public String next() { return calls++ < 2 ? first : second; }
        };
        var service = new UrlService(urls, policy, generator, config, clock, transactions, daily, keys);
        var request = new CreateUrlRequest("https://example.com/collision", null, null, null);
        assertThat(service.create(request, null).url().code()).isEqualTo(first);
        assertThat(service.create(request, null).url().code()).isEqualTo(second);
        assertThatThrownBy(() -> service.create(request, null)).hasMessageContaining("Unable to allocate");
        assertThat(urls.findById(first)).isPresent();
    }
}
