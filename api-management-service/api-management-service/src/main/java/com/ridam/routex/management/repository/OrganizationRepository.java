package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationSlugProjection;
import com.ridam.routex.management.entity.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization,Long> {

    public Optional<Organization> findById(Long id);

    @Query("""
            SELECT o.id AS id, o.slug AS slug
            FROM Organization o
            WHERE o.id IN :orgIds
        """)
    List<OrganizationSlugProjection> findAllSlugById(
            @Param("orgIds") List<Long> orgIds
    );

    Optional<Organization> findByIdAndStatusIsNot(Long id, OrganizationStatus status);

    public boolean existsByNameIgnoreCaseAndSlugIgnoreCase(String name,String slug);

    public boolean existsBySlugIgnoreCaseAndNameNotIgnoreCase(String slug,String name);

    public boolean existsByNameAndIdNot(String name,Long id);

    public boolean existsBySlugIgnoreCaseAndIdNot(String name,Long id);

    public boolean existsByNameIgnoreCase(String name);


    Optional<Organization> findByIdAndStatusNot(Long id, OrganizationStatus organizationStatus);
}
