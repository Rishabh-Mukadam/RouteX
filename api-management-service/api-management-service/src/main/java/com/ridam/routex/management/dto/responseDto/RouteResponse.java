package com.ridam.routex.management.dto.responseDto;


import com.ridam.routex.management.entity.ApiRoute;
import com.ridam.routex.management.entity.RouteHeaderRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class RouteResponse {

    protected Long id;
    protected Long organizationId;
    protected String routeId;
    protected String name;
    protected String pathPattern;
    protected String targetBaseUrl;
    protected String targetPathPrefix;
    protected Integer requestsPerMinute;
    protected Integer responseTimeoutMs;
    protected boolean idempotencyEnabled;
    protected boolean active;
    protected Long configurationVersion;
    protected Set<String> allowedMethods;
    protected List<HeaderRuleResponse> headerRules;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public static RouteResponse from(ApiRoute route) {
        List<HeaderRuleResponse> rules = new ArrayList<HeaderRuleResponse>();
        for(RouteHeaderRule rule : route.getHeaderRules()) {
            rules.add(HeaderRuleResponse.from(rule));
        }

        return new RouteResponse(route.getId(), route.getOrganization().getId(), route.getRouteId(),
                route.getName(), route.getPathPattern(), route.getTargetBaseUrl(), route.getTargetPathPrefix(),
                route.getRequestsPerMinute(), route.getResponseTimeoutMs(), route.isIdempotencyEnabled(),
                route.isActive(), route.getConfigurationVersion(), route.getAllowedMethods(), rules,
                route.getCreatedAt(), route.getUpdatedAt());
    }
}