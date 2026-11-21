//package com.ridam.routex.gateway.exception;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//@RestControllerAdvice
//public class GlobalException {
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> runtimeexception(Exception e){
//        return ResponseEntity.internalServerError().body(e.getMessage());
//    }
//}
