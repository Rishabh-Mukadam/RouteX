package com.ridam.routex.gateway.service;

import com.ridam.routex.gateway.dto.CompiledRoute;
import com.ridam.routex.gateway.dto.GateWayErrorResponse;
import com.ridam.routex.gateway.dto.GatewayRoute;
import com.ridam.routex.gateway.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class GatewayService {

    private final RouteRegistry routeRegistry;
    private final ObjectMapper objectMapper;

    public GatewayService(RouteRegistry routeRegistry, ObjectMapper objectMapper) {
        this.routeRegistry = routeRegistry;
        this.objectMapper = objectMapper;
    }


    public ResponseEntity<byte[]> handle(HttpServletRequest request) {
//        1.get compiled route for incomming http request

        Optional<CompiledRoute> matchedRoute=routeRegistry.find(request.getRequestURI());

        if(!matchedRoute.isPresent()){
            return error(
                    HttpStatus.NOT_FOUND,
                    "Route does not exists in registry",
                    "ok",
                    null);

        }

        CompiledRoute compiledRoute=matchedRoute.get();
        GatewayRoute gatewayRoute=compiledRoute.getGatewayRoute();


//        chekc if the method is allow or now
        if(!gatewayRoute.getAllowedMethods().contains(request.getMethod())){
            HttpHeaders headers=new HttpHeaders();
            headers.set(HttpHeaders.ALLOW,String.join(",",gatewayRoute.getAllowedMethods()));
            return error(
                    HttpStatus.BAD_REQUEST,
                    request.getMethod()+" method is not allow for this path",
                    "ok",
                    headers);

        }

//        2.To find rate limiting rule( radis)

//        3.Idempotency check
//        4.Header rule on request
//        5.Redirect end point to the target
//        6.Header rules on response
//        7.Stor response to cache if idempotency is enabled
//        8.Return result to the client



        return ResponseEntity
                .status(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(compiledRoute.getPathPattern().toString().getBytes());
    }

    private ResponseEntity<byte[]> error(
            HttpStatus status,
            String message,
            String correlationId,
            HttpHeaders additionalHeader
    ){
        GateWayErrorResponse errorResponse=new GateWayErrorResponse();
        errorResponse.setCorrelationId(correlationId);
        errorResponse.setMessage(message);
        HttpHeaders headers=additionalHeader==null?new HttpHeaders():additionalHeader;

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X_Correlation_id",correlationId);

        return ResponseEntity.status(status)
                .headers(headers)
                .body(objectMapper.writeValueAsString(errorResponse).getBytes(StandardCharsets.UTF_8));
    }
}
