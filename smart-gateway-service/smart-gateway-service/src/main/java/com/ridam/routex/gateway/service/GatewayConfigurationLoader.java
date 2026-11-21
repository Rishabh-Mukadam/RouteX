package com.ridam.routex.gateway.service;

import com.ridam.routex.gateway.client.ManagementApiClient;
import com.ridam.routex.gateway.dto.GatewayRoute;

import java.util.List;

public class GatewayConfigurationLoader implements Runnable{

    private final ManagementApiClient managementApiClient;

    public GatewayConfigurationLoader(ManagementApiClient managementApiClient) {
        this.managementApiClient = managementApiClient;
    }

    @Override
    public void run() {
        List<GatewayRoute> routes=managementApiClient.fetchActiveRoutes();
    }
}
