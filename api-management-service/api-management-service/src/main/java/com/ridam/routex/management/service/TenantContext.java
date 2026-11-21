package com.ridam.routex.management.service;


import com.ridam.routex.management.controller.TenantOrganizationController;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class TenantContext {

    OrganizationRepository repository;

    public TenantContext(OrganizationRepository repository){
        this.repository=repository;
    }

    public Organization getOrganization(){
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();

        if(!(authentication instanceof JwtAuthenticationToken)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }
        Long organization_id=((JwtAuthenticationToken) authentication).getToken().getClaim("organizationId");

        if(organization_id == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant identity is missing");
        }

        Organization organization=repository.findById(organization_id)
                .orElseThrow(()->new ApiException(HttpStatus.FORBIDDEN, "Organization does not exist"));

        return organization;
    }
}
