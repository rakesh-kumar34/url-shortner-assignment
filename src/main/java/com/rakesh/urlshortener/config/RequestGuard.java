package com.rakesh.urlshortener.config;

import com.rakesh.urlshortener.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;
import tools.jackson.databind.ObjectMapper;

@Component @Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestGuard extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestGuard.class);
    private static final int MAX_BODY = 8192;
    private final AppProperties config;
    private final RateLimiter limiter;
    private final ObjectMapper json;

    public RequestGuard(AppProperties config, RateLimiter limiter, ObjectMapper json) {
        this.config = config; this.limiter = limiter; this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute("requestId", id);
        response.setHeader("X-Request-ID", id);
        response.setHeader("Cache-Control", "no-store");
        long start = System.nanoTime();
        // Match decoded path segments just as Spring MVC does, including a context path.
        String prefix = ServletRequestPathUtils.parse(request).pathWithinApplication().elements().stream()
                .filter(PathContainer.PathSegment.class::isInstance).map(PathContainer.PathSegment.class::cast)
                .map(PathContainer.PathSegment::valueToMatch).findFirst().orElse("");
        String route = prefix.equals("api") ? "management" : prefix.equals("s") ? "redirect" : "other";
        try {
            if (!route.equals("other")) {
                int retry = limiter.acquire(route + ':' + request.getRemoteAddr(),
                        route.equals("management") ? config.managementLimit() : config.redirectLimit());
                if (retry > 0) {
                    response.setHeader("Retry-After", Integer.toString(retry));
                    reject(response, 429, "rate_limited", "Request limit reached; retry later", id); return;
                }
            }
            if (route.equals("management") && !request.getMethod().equals("GET") && !request.getMethod().equals("HEAD")) {
                String encoding = request.getHeader("Content-Encoding");
                if (encoding != null && !encoding.equalsIgnoreCase("identity")) {
                    reject(response, 415, "unsupported_encoding", "Compressed request bodies are not supported", id); return;
                }
                if (request.getContentLengthLong() > MAX_BODY) {
                    reject(response, 413, "body_too_large", "JSON body must not exceed 8 KiB", id); return;
                }
                byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
                if (bytes.length > MAX_BODY) {
                    reject(response, 413, "body_too_large", "JSON body must not exceed 8 KiB", id); return;
                }
                request = new BufferedRequest(request, bytes);
            }
            chain.doFilter(request, response);
        } finally {
            // Deliberately omit raw path, query, destination, IP, headers and body.
            LOG.info("request_id={} route={} method={} status={} duration_ms={}", id, route,
                    request.getMethod(), response.getStatus(), (System.nanoTime() - start) / 1_000_000);
        }
    }

    private void reject(HttpServletResponse response, int status, String code, String message, String id) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        json.writeValue(response.getOutputStream(), new ApiError(code, message, id));
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        BufferedRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous body only"); }
            };
        }
        @Override public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
