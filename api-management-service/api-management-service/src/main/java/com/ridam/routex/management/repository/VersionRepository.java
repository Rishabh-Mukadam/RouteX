package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.RouteConfigurationVersion;
import com.ridam.routex.management.entity.RouteHeaderRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionRepository extends JpaRepository<RouteConfigurationVersion,Long> {
}
