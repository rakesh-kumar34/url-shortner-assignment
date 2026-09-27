package com.rakesh.urlshortener.api;

import com.rakesh.urlshortener.link.LinkService;
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
public class LinkController {
    private final LinkService service;
    public LinkController(LinkService service) { this.service = service; }

    @PostMapping("/api/links")
    ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var result = service.create(body, key);
        return ResponseEntity.status(result.replayed() ? 200 : 201)
                .location(URI.create("/api/links/" + result.link().code())).body(result.link());
    }

    @GetMapping("/api/links")
    LinkService.LinkPage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/api/links/{code}")
    LinkResponse get(@PathVariable String code) { return service.get(code); }

    @GetMapping("/api/links/{code}/stats")
    LinkService.Stats stats(@PathVariable String code) { return service.stats(code); }

    @DeleteMapping("/api/links/{code}")
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
