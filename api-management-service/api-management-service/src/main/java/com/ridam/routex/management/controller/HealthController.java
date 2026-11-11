package com.ridam.routex.management.controller;


import com.ridam.routex.management.dto.responseDto.HealthResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    private String serviceName;
    private String serviceDescription;

    @GetMapping
    public ResponseEntity<HealthResponseDto> health(
            @Value("${info.service.name}") String serviceName,
            @Value("${info.service.description}") String serviceDescription
            ){
        return ResponseEntity.ok(
                new HealthResponseDto(serviceName,serviceDescription,"UP")
        );
    }
}
