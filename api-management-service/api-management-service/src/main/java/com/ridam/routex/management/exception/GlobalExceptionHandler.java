package com.ridam.routex.management.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException exception){
        return ResponseEntity.status(exception.getStatus()).body(exception.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> runtimeException(Exception exception){
        return ResponseEntity.status(500).body("Something went wrong "+exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<HashMap<String,String>> methodArgumentNotValidException(MethodArgumentNotValidException exception){
        HashMap<String,String> validationError=new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error->
                        validationError.put(error.getField(),error.getDefaultMessage())
                        );

        return ResponseEntity.status(500).body(validationError);
    }
}
