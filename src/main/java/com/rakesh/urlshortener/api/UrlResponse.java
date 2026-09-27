package com.rakesh.urlshortener.api;

import java.time.Instant;

public record UrlResponse(String code, String shortUrl, String url, String title,
                           Instant createdAt, long totalClicks, Instant lastClickedAt,
                           Instant expiresAt, Instant disabledAt, String status) { }
