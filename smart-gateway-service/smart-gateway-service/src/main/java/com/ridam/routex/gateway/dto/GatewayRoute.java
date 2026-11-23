package com.ridam.routex.gateway.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class GatewayRoute {
    private Long id;
    private Long organizationId;
    private String slug;
    private String routeId;
    private String name;
    private String pathPattern;
    private String targetBaseUrl;
    private String targetPathPrefix;
    private Integer requestsPerMinute;
    private Integer responseTimeoutMs;
    private boolean idempotencyEnabled;
    private boolean active;
    private Long configurationVersion;
    private Set<String> allowedMethods = new HashSet<String>();
    private List<HeaderRule> headerRules = new ArrayList<HeaderRule>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
