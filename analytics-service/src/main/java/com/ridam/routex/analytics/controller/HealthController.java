package com.ridam.routex.analytics.controller;

import com.ridam.routex.analytics.dto.ResponseDto.HealthResponseDto;
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

    public HealthController(
            @Value("${info.service.name}") String serviceName,
            @Value("${info.service.description}") String serviceDescription
    ){
        this.serviceName=serviceName;
        this.serviceDescription=serviceDescription;
    }

    @GetMapping
    public ResponseEntity<HealthResponseDto> health(){
        return ResponseEntity.ok(
                new HealthResponseDto(serviceName,serviceDescription,"UP")
        );
    }
}
