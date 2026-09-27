package com.rakesh.urlshortener.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String publicOrigin, String apiToken, int managementLimit, int redirectLimit) {
    public AppProperties {
        if (apiToken == null || apiToken.length() < 32) {
            throw new IllegalArgumentException("API_TOKEN must contain at least 32 characters");
        }
        URI origin = URI.create(publicOrigin);
        if (!("https".equals(origin.getScheme()) || "http".equals(origin.getScheme()))
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getRawQuery() != null || origin.getRawFragment() != null
                || !(origin.getPath().isEmpty() || origin.getPath().equals("/"))) {
            throw new IllegalArgumentException("PUBLIC_ORIGIN must be an HTTP(S) origin without a path");
        }
        publicOrigin = publicOrigin.replaceAll("/+$", "");
        if (managementLimit < 1 || redirectLimit < 1) {
            throw new IllegalArgumentException("Rate limits must be positive");
        }
    }
}
