package com.rakesh.shortline.api;

public record ApiError(String code, String message, String requestId) { }
