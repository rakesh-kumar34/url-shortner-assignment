package com.rakesh.shortline.api;

import java.time.Instant;

public record LinkResponse(String code, String shortUrl, String url, String title,
                           Instant createdAt, long totalClicks, Instant lastClickedAt) { }
