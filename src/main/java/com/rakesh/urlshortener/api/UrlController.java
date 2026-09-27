package com.rakesh.urlshortener.api;

import com.rakesh.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController {
    private final UrlService service;
    public UrlController(UrlService service) { this.service = service; }

    @PostMapping({"/api/urls", "/api/links"})
    ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var result = service.create(body, key);
        return ResponseEntity.status(result.replayed() ? 200 : 201)
                .location(URI.create("/api/urls/" + result.url().code())).body(result.url());
    }

    @GetMapping({"/api/urls", "/api/links"})
    UrlService.UrlPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping({"/api/urls/{code}", "/api/links/{code}"})
    UrlResponse get(@PathVariable String code) { return service.get(code); }

    @GetMapping({"/api/urls/{code}/stats", "/api/links/{code}/stats"})
    UrlService.Stats stats(@PathVariable String code) { return service.stats(code); }

    @DeleteMapping({"/api/urls/{code}", "/api/links/{code}"})
    ResponseEntity<Void> disable(@PathVariable String code) {
        service.disable(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/s/{code}")
    ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        return ResponseEntity.status(302).header("Cache-Control", "no-store")
                .location(URI.create(service.resolve(code, !request.getMethod().equals("HEAD")))).build();
    }
}
