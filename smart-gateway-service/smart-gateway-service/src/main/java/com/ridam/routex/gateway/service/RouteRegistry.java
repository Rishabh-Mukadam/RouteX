package com.ridam.routex.gateway.service;


import com.ridam.routex.gateway.dto.CompiledRoute;
import com.ridam.routex.gateway.dto.GatewayRoute;
import com.ridam.routex.gateway.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPattern;

import javax.net.ssl.HttpsURLConnection;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteRegistry {

    private Map<String, List<CompiledRoute>> compile_routes=new HashMap<>();

    public void replaceAll(Map<String, List<GatewayRoute>> gatewayRoutesMap){
        gatewayRoutesMap.forEach((orgSlug,orgRoutes)->{
            List<CompiledRoute> compiledRouteslist=orgRoutes
                    .stream()
                    .map(CompiledRoute::fromGatewayRoute)
                    .collect(Collectors.toCollection(ArrayList::new));

            Collections.sort(
                    compiledRouteslist,
                    new Comparator<CompiledRoute>() {
                        @Override
                        public int compare(CompiledRoute o1, CompiledRoute o2) {
                            return PathPattern.SPECIFICITY_COMPARATOR.compare(o1.getPathPattern(),o2.getPathPattern());
                        }
                    }
            );
            compile_routes.put(orgSlug,compiledRouteslist);
        });
        System.out.println("all replacement is ok");
    }

    public Optional<CompiledRoute> find(String requestPath){
        String slug =requestPath.split("/")[1];
        if(slug.isEmpty()){
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Org does not exists"
            );
        }
        List<CompiledRoute> org_routes=compile_routes.get(slug);
        if(org_routes==null){
            return Optional.empty();
        }
        PathContainer pathContainer=PathContainer.parsePath(requestPath);
        for(CompiledRoute compiledRoute:org_routes){
            if(compiledRoute.getPathPattern().matches(pathContainer)){
                return Optional.of(compiledRoute);
            }
        }
        return Optional.empty();
    }

}
