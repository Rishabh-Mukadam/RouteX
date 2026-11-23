package com.ridam.routex.management.service;

import com.ridam.routex.management.dto.requestDto.HeaderRuleRequest;
import com.ridam.routex.management.dto.requestDto.RouteRequest;
import com.ridam.routex.management.dto.requestDto.RouteStatusRequest;
import com.ridam.routex.management.dto.responseDto.AuditRecordResponse;
import com.ridam.routex.management.dto.responseDto.GatewayRouteResponse;
import com.ridam.routex.management.dto.responseDto.RouteResponse;
import com.ridam.routex.management.dto.responseDto.RouteVersionResponse;
import com.ridam.routex.management.entity.*;
import com.ridam.routex.management.enums.RouteVersionChnageType;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.*;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteService {
    private final TenantContext tenantContext;
    private final ApiRouteRepository apiRouteRepository;
    private final VersionRepository versionRepository;
    private final ObjectMapper objectMapper;
    private final RouteConfigurationVersionRepository routeConfigurationVersionRepository;
    private final AuditService auditService;
    private final ControlPlaneAuditRecordRepository auditRecordRepository;
    private final JdbcTemplate template;
    private final OrganizationRepository organizationRepository;


    public RouteService(TenantContext tenantContext, ApiRouteRepository apiRouteRepository, VersionRepository versionRepository, ObjectMapper objectMapper, RouteConfigurationVersionRepository routeConfigurationVersionRepository, AuditService auditService, ControlPlaneAuditRecordRepository auditRecordRepository, JdbcTemplate template, OrganizationRepository organizationRepository) {
        this.tenantContext = tenantContext;
        this.apiRouteRepository = apiRouteRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
        this.routeConfigurationVersionRepository = routeConfigurationVersionRepository;
        this.auditService = auditService;
        this.auditRecordRepository = auditRecordRepository;
        this.template = template;
        this.organizationRepository = organizationRepository;
    }

    public RouteResponse create(@Valid RouteRequest routeRequest) {

        long startedAt = System.nanoTime();

        Organization organization=tenantContext.getOrganization();
        validate(routeRequest,organization);

        if(apiRouteRepository.existsByOrganizationIdAndRouteId(organization.getId(),routeRequest.getRouteId())){
            throw new ApiException(
                    HttpStatus.CONFLICT,"Route ID already exists for this organization"
            );
        }

        ApiRoute route = new ApiRoute();
        route.setOrganization(organization);
        route.setConfigurationVersion(1L);
        route.setCreatedAt(LocalDateTime.now());
        apply(route, routeRequest);
        apiRouteRepository.save(route);

        saveVersion(route,RouteVersionChnageType.CREATED);


        auditService.record(organization, "CREATE_ROUTE",
                "API_ROUTE", route.getRouteId(), startedAt,
                "Route configuration version 1 created");
        
        return RouteResponse.from(route);

    }



    private Set<String> normalizeMethods(Set<String> methods) {
        Set<String> normalized = new HashSet<String>();
        for(String method : methods) {
            String value = method.toUpperCase(Locale.ENGLISH);
            try {
                HttpMethod.valueOf(value);
            }
            catch(IllegalArgumentException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method: " + method);
            }
            normalized.add(value);
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getAuditRecords() {
        Organization organization = tenantContext.getOrganization();

        List<AuditRecordResponse> response = new ArrayList<AuditRecordResponse>();

        List<ControlPlaneAuditRecord> records = auditRecordRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(organization.getId());

        for(ControlPlaneAuditRecord record : records) {
            response.add(AuditRecordResponse.from(record));
        }
        return response;
    }


    private void apply(ApiRoute route, RouteRequest request) {
        route.setRouteId(request.getRouteId());
        route.setName(request.getName());
        route.setPathPattern(request.getPathPattern());
        route.setTargetBaseUrl(request.getTargetBaseUrl());
        route.setTargetPathPrefix(request.getTargetPathPrefix());
        route.setRequestsPerMinute(request.getRequestsPerMinute());
        route.setResponseTimeoutMs(request.getResponseTimeoutMs());
        route.setIdempotencyEnabled(request.isIdempotencyEnabled());
        route.setActive(request.isActive());
        route.setUpdatedAt(LocalDateTime.now());
        route.setAllowedMethods(normalizeMethods(request.getAllowedMethods()));

        route.getHeaderRules().clear();
        for(HeaderRuleRequest ruleRequest : request.getHeaderRules()) {
            RouteHeaderRule rule = new RouteHeaderRule();
            rule.setRoute(route);
            rule.setDirection(ruleRequest.getDirection().toUpperCase(Locale.ENGLISH));
            rule.setAction(ruleRequest.getAction().toUpperCase(Locale.ENGLISH));
            rule.setHeaderName(ruleRequest.getHeaderName());
            rule.setHeaderValue(ruleRequest.getHeaderValue());
            rule.setCreatedAt(LocalDateTime.now());
            rule.setUpdatedAt(LocalDateTime.now());
            route.getHeaderRules().add(rule);
        }
    }


    private void validate(RouteRequest routeRequest,Organization organization){
        String tenatPrefix="/"+organization.getSlug()+"/";

//        URL syntax validation
        if(!routeRequest.getPathPattern().startsWith(tenatPrefix)){
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "pathPattern must start with the tenant prefix \" + tenantPrefix"
            );
        }

        if(!routeRequest.getTargetPathPrefix().startsWith("/")){
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,"targetPathPrefix must start with /"
            );
        }


        //URL validations
        URI uri=URI.create(routeRequest.getTargetBaseUrl());
        try{
            if(!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
        }
        catch(IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "targetBaseUrl must be a valid HTTP or HTTPS URL");
        }

        //rest method validation
        routeRequest.getAllowedMethods().forEach(method->{
            try{
                HttpMethod.valueOf(method);
            }catch (Exception e){
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported HTTP method: " + method
                );
            }
        });

        routeRequest.getHeaderRules().forEach(rule -> {
            String direction =rule.getDirection();
            String action =rule.getAction();

            if (!"REQUEST".equals(direction) && !"RESPONSE".equals(direction)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Header direction must be REQUEST or RESPONSE"
                );
            }

            if(!"ADD".equals(action) && !"REMOVE".equals(action)){
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Header action must be ADD or REMOVE");
            }

            if("ADD".equals(action) &&
                    (rule.getHeaderName()==null)|| rule.getHeaderValue()==null || rule.getHeaderValue().trim().isEmpty()){
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "header value must not be empty"
                );
            }

        });



    }

    public List<RouteResponse> findAllForTenant() {
        Organization organization=tenantContext.getOrganization();

        List<ApiRoute> allRoutes=apiRouteRepository.findAllByOrganizationId(organization.getId());
        return allRoutes.stream().
                map(apiRoute->RouteResponse.from(apiRoute))
                .toList();

    }

    public RouteResponse findById(Long id) {
        Organization org=tenantContext.getOrganization();
        ApiRoute route= apiRouteRepository.findByIdAndOrganizationId(id,org.getId()).orElseThrow(()->{
            return new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Route not exist with id : "+id
            );
        });
        return RouteResponse.from(route);
    }

    public void delete(Long id) {

        long startedAt=System.nanoTime();
        Organization org=tenantContext.getOrganization();
        ApiRoute route= apiRouteRepository.findByIdAndOrganizationId(id,org.getId()).orElseThrow(()->{
            return new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Route not exist with id : "+id
            );
        });
        saveVersion(route,RouteVersionChnageType.DELETED);
        auditService.record(org, "DELETE_ROUTE", "API_ROUTE", route.getRouteId(), startedAt,
                "Final configuration saved as version " + route.getConfigurationVersion());

        apiRouteRepository.delete(route);
    }

    public RouteResponse changeStatus(Long id, @Valid RouteStatusRequest request) {

        long startedAt=System.nanoTime();
        Organization org= tenantContext.getOrganization();


        ApiRoute route=apiRouteRepository.findByIdAndOrganizationId(id,org.getId()).orElseThrow(()->{
           return new ApiException(HttpStatus.NOT_FOUND,"Route is not exists ");
        });
        String action = request.getActive() ? "ACTIVATE_ROUTE" : "DEACTIVATE_ROUTE";

        route.setActive(request.getActive());
        route.setUpdatedAt(LocalDateTime.now());
        route.setConfigurationVersion(route.getConfigurationVersion()+1);

        saveVersion(route,route.isActive()?RouteVersionChnageType.ACTIVATED:RouteVersionChnageType.DEACTIVATED);
        auditService.record(org, action, "API_ROUTE", route.getRouteId(), startedAt,
                "Route configuration version " + route.getConfigurationVersion() + " created");

        return RouteResponse.from(route);
    }

    public RouteResponse update(Long id, @Valid RouteRequest request) {
        long startedAt = System.nanoTime();

        Organization org=tenantContext.getOrganization();
        ApiRoute route=apiRouteRepository.findByIdAndOrganizationId(id,org.getId()).orElseThrow(()->
        {
            return new ApiException(
                    HttpStatus.NOT_FOUND,
                    "there no route exists with id :"+id
            );
        });
        if(!route.getRouteId().equals(request.getRouteId())){
            throw new ApiException(HttpStatus.BAD_REQUEST, "routeId cannot be changed");
        }
        validate(request,org);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        apply(route,request);

        saveVersion(route,RouteVersionChnageType.DELETED);

        auditService.record(org, "UPDATE_ROUTE", "API_ROUTE",
                route.getRouteId(), startedAt, "Route configuration version " +
                        route.getConfigurationVersion() + " created");


        return RouteResponse.from(route);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> findAllActiveApiRoutes() {
        List<ApiRoute> list=apiRouteRepository.findAllByActiveTrueAndOrganizationStatus(OrganizationStatus.ACTIVE);
        if(list.isEmpty()){
            System.out.println("no routes are there");
        }

        return list.stream()
                .map(RouteResponse::from).toList();
    }



        public List<GatewayRouteResponse> findAllGatewayRoutes(){
    //        String sql= """
    //                SELECT r.*, o.slug AS slug
    //                            FROM api_routes r
    //                            JOIN organizations o ON r.organization_id = o.id
    //                            WHERE r.active = true
    //                            and o.status = 'ACTIVE'
    //
    //                """;
            List<RouteResponse> routeResponseList=findAllActiveApiRoutes();
            List<OrganizationSlugProjection> organizationSlugProjectionlist=organizationRepository.findAllSlugById(
                    routeResponseList.stream()
                            .map(RouteResponse::getOrganizationId)
                            .distinct()
                            .toList()
            );

            Map<Long,String> slugMap=organizationSlugProjectionlist.stream()
                    .collect(Collectors.toMap(
                            OrganizationSlugProjection::getId,
                            OrganizationSlugProjection::getSlug
                    ));

            List<GatewayRouteResponse> res=routeResponseList.stream()
                    .map(routeResponse -> {
                        GatewayRouteResponse gatewayRouteResponse=new GatewayRouteResponse();
                        BeanUtils.copyProperties(routeResponse, gatewayRouteResponse);
                        gatewayRouteResponse.setSlug(slugMap.get(routeResponse.getOrganizationId()));
                        return gatewayRouteResponse;
                    }).toList();

            return res;
        }


    private void saveVersion(ApiRoute route, RouteVersionChnageType changeType){
        RouteConfigurationVersion version=new RouteConfigurationVersion();
        version.setOrganization(route.getOrganization());
        version.setRouteId(route.getRouteId());
        version.setVersionNumber(route.getConfigurationVersion());
        version.setChangeType(changeType.toString());
        version.setConfigurationSnapshot(objectMapper.writeValueAsString(RouteResponse.from(route)));
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);

    }

    public List<RouteVersionResponse> getVersions(String routeId) {
        Organization organizatio=tenantContext.getOrganization();

        return routeConfigurationVersionRepository
                .findAllByOrganizationIdAndRouteIdOrderByVersionNumberDesc(organizatio.getId(),routeId)
                .stream()
                .map(RouteVersionResponse::from).toList();

    }
}
