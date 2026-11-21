package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.ApiRoute;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRouteRepository extends JpaRepository<ApiRoute,Long> {
    public boolean existsByOrganizationIdAndRouteId(Long organization_id, String routeId);

    public Optional<ApiRoute> findByIdAndOrganizationId(Long id,Long orgId);

    public List<ApiRoute> findAllByOrganizationId(Long id);

    public List<ApiRoute> findAllByActiveTrueAndOrganizationStatus(OrganizationStatus status);
}
