package com.rakesh.urlshortener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.management-limit=2", "app.redirect-limit=1"})
@ActiveProfiles("test")
class RequestBoundaryIntegrationTest {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test void encodedRoutesShareBodyAndRateLimitsWithCanonicalRoutes() throws Exception {
        var oversized = request("/a%70i/urls").POST(HttpRequest.BodyPublishers.ofString(
                "{\"url\":\"https://example.com\"}" + " ".repeat(9000))).build();
        assertThat(client.send(oversized, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(413);
        assertThat(get("/api/urls").statusCode()).isEqualTo(200);
        var limited = get("/a%70i/urls");
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Retry-After")).isPresent();
        assertThat(get("/%73/no-such-link").statusCode()).isEqualTo(404);
        assertThat(get("/s/no-such-link").statusCode()).isEqualTo(429);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", CoreIntegrationTest.AUTH).header("Content-Type", "application/json");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
