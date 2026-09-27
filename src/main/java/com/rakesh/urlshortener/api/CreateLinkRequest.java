package com.rakesh.urlshortener.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 120) String title,
        @Pattern(regexp = "[A-Za-z0-9_-]{4,32}") String customAlias,
        Instant expiresAt) { }
