package com.rakesh.urlshortener.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> known(ApiException e, HttpServletRequest request) {
        return response(e.status(), e.code(), e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpServletRequest request) {
        return response(400, "invalid_json", "Invalid JSON, unknown field or invalid field type", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> validation(HttpServletRequest request) {
        return response(422, "validation_error", "One or more fields are invalid", request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> database(HttpServletRequest request) {
        return ResponseEntity.status(503).header("Retry-After", "1")
                .body(new ApiError("storage_unavailable", "Storage temporarily unavailable", id(request)));
    }

    private ResponseEntity<ApiError> response(int status, String code, String message, HttpServletRequest r) {
        return ResponseEntity.status(status).body(new ApiError(code, message, id(r)));
    }

    private String id(HttpServletRequest request) {
        return (String) request.getAttribute("requestId");
    }
}
