package com.ridam.routex.analytics.dto.ResponseDto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HealthResponseDto {
    private String serviceName;
    private String serviceDescription;
    private String appStatus;

}
