package com.ridam.routex.gateway.client;


import com.ridam.routex.gateway.dto.GatewayRoute;
import com.ridam.routex.gateway.dto.LoginRequest;
import com.ridam.routex.gateway.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Component
public class ManagementApiClient {

    private final String username;
    private final String password;
    private final RestClient restClient;
    private String token;
    private Instant expiresAt;


    public ManagementApiClient(@Value("${management.api.username}") String username,
                               @Value("${management.api.password}") String password,
                               @Value("${management.api.baseUrl}") String baseUrl) {
        this.restClient=RestClient.builder().baseUrl(baseUrl).build();
        this.username = username;
        this.password = password;
    }

    public List<GatewayRoute> fetchActiveRoutes(){
        ensureToken();
        List<GatewayRoute> gatewayRouteList;
        try{
            gatewayRouteList=restClient.get()
                    .uri("/api/gateway/routes")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GatewayRoute>>() {});

        }catch (Exception e){
            throw e;
        }
        if(gatewayRouteList.isEmpty() || gatewayRouteList==null){
        }
        return gatewayRouteList;
    }

    private synchronized void ensureToken() {
        if(token==null || expiresAt==null || Instant.now().plusSeconds(30).isAfter(expiresAt)){
            authenticat();
        }
    }

    private synchronized void authenticat() {
        LoginResponse loginResponse=restClient.post()
                .uri("/api/auth/login")
                .body(new LoginRequest(username,password))
                .retrieve()
                .body(LoginResponse.class);

        this.token=loginResponse.getToken();
    }
}
