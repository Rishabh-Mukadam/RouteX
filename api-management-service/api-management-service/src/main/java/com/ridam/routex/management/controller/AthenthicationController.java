package com.ridam.routex.management.controller;


import com.ridam.routex.management.dto.requestDto.LoginRequest;
import com.ridam.routex.management.dto.responseDto.LoginResponse;
import com.ridam.routex.management.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AthenthicationController {

    private AuthenticationService authenticationService;
    public AthenthicationController(AuthenticationService authenticationService){
        this.authenticationService=authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authenticationService.login(request));

    }

    @GetMapping("/getCSRF")
    public CsrfToken getCsrf(CsrfToken csrfToken){
        return csrfToken;
    }
}
