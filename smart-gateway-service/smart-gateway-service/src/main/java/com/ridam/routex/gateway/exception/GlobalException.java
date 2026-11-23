package com.ridam.routex.gateway.exception;

import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalException {

    private final ResourceLoader resourceLoader;

    public GlobalException(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> runtimeexception(Exception e) {
        return ResponseEntity.internalServerError().body(e.getMessage());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException exception){
        return ResponseEntity.status(exception.getStatus()).body(exception.getMessage());
    }
}
