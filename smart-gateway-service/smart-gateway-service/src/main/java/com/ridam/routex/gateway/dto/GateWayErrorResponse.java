package com.ridam.routex.gateway.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class GateWayErrorResponse {
    private int status;
    private String message;
    private String correlationId;
    private LocalTime timestamp;

}
