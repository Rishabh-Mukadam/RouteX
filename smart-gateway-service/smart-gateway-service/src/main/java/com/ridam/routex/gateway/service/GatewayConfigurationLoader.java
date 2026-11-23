package com.ridam.routex.gateway.service;

import com.ridam.routex.gateway.client.ManagementApiClient;
import com.ridam.routex.gateway.dto.GatewayRoute;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GatewayConfigurationLoader implements ApplicationRunner {

    private final ManagementApiClient managementApiClient;
    private final RouteRegistry routeRegistry;

    public GatewayConfigurationLoader(ManagementApiClient managementApiClient, RouteRegistry routeRegistry) {
        this.managementApiClient = managementApiClient;
        this.routeRegistry = routeRegistry;
    }
//
//    @Override
//    public void run() {
//    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String,List<GatewayRoute>> routes=managementApiClient.fetchActiveRoutes();
        routeRegistry.replaceAll(routes);
    }
}
