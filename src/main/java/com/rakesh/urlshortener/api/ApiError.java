package com.rakesh.urlshortener.api;

public record ApiError(String code, String message, String requestId) { }
