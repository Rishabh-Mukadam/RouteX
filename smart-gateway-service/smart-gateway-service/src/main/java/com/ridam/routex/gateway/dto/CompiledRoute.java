package com.ridam.routex.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.concurrent.CompletableFuture;


@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class CompiledRoute {

    private GatewayRoute gatewayRoute;
    private PathPattern pathPattern;
    private String sourcePathPrefix;
    private static PathPatternParser pathPatternParser=new PathPatternParser();

    public static CompiledRoute fromGatewayRoute(GatewayRoute gatewayRoute){

        if (gatewayRoute==null){
            throw new RuntimeException("Gateway route is null");
        }
        CompiledRoute compiledRoute=new CompiledRoute();

        compiledRoute.setGatewayRoute(gatewayRoute);

        PathPattern pattern=pathPatternParser.parse(gatewayRoute.getPathPattern());
        compiledRoute.setPathPattern(pattern);
        compiledRoute.setSourcePathPrefix(
                getSourcePathPrefix(gatewayRoute.getPathPattern())
        );
        return compiledRoute;

    }

    private static String getSourcePathPrefix(String pathPattern){
        if(pathPattern.endsWith("/*")){
            return pathPattern.substring(0,pathPattern.length()-2);
        }else if(pathPattern.endsWith("/**")){
            return pathPattern.substring(0,pathPattern.length()-3);
        }
        return pathPattern;
    }
}
