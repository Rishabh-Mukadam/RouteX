package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization,Long> {

    public Optional<Organization> findById(Long id);

    public boolean existsByNameIgnoreCaseAndSlugIgnoreCase(String name,String slug);

    public boolean existsBySlugIgnoreCaseAndNameNotIgnoreCase(String slug,String name);

    public boolean existsByNameAndIdNot(String name,Long id);

    public boolean existsBySlugIgnoreCaseAndIdNot(String name,Long id);

}
