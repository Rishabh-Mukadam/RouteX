package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.RouteConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteConfigurationVersionRepository extends JpaRepository<RouteConfigurationVersion,Long> {
    List<RouteConfigurationVersion> findAllByOrganizationIdAndRouteIdOrderByVersionNumberDesc(Long organizationId, String routeId);
}
