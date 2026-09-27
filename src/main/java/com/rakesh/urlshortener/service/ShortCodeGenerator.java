package com.rakesh.urlshortener.service;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {
    private final SecureRandom random = new SecureRandom();

    public String next() {
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
