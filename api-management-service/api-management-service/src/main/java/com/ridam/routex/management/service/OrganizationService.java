package com.ridam.routex.management.service;

import com.ridam.routex.management.dto.requestDto.OrganizationRequest;
import com.ridam.routex.management.dto.responseDto.OrganizationResponse;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AutoPopulatingList;

import java.awt.print.PrinterException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class OrganizationService {

    private OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository){
        this.repository=repository;
    }

    @Transactional
    public Organization create(OrganizationRequest request) {
//        check if already exist with same name
        boolean exists=repository.existsByNameIgnoreCaseAndSlugIgnoreCase(request.getName(),request.getSlug());
        if(exists){
            throw new ApiException(HttpStatus.CONFLICT,"Organization already exists");
        }
        Organization saved_org= repository.save(Organization.from(request));
        return saved_org;
    }

    @Transactional
    public List<OrganizationResponse> findAll() {

        List<Organization> list=repository.findAll();
        return list.stream().map(organization -> {
            return OrganizationResponse.from(organization);
        }).toList();
    }

    @Transactional
    public OrganizationResponse findById(Long id) {
        Organization org= repository.findById(id).orElseThrow(()->new ApiException(
                HttpStatus.NOT_FOUND,
                "Data not found"
        ));
        return OrganizationResponse.from(org);
    }

    @Transactional
    public Organization update(Long id,OrganizationRequest request) {
//        return existing_org;

        Organization current=repository.findById(id).orElseThrow(()->
                new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Organization ID not exist in table"
                )
        );

        if(repository.existsBySlugIgnoreCaseAndIdNot(request.getName(),id)){
            throw new ApiException(
                    HttpStatus.CONFLICT,"Slug is already exists"
            );
        }

        current.setUpdatedAt(LocalDateTime.now());
        current.setName(request.getName());
        current.setSlug(request.getSlug());

        return current;
    }

    @Transactional
    public Organization changeStatus(Long id, OrganizationStatus status) {

        long startedAt = System.nanoTime();
        Organization organization = repository.findById(id).orElseThrow(
                ()->new ApiException(HttpStatus.NOT_FOUND,"Organization not found")
        );

        if(status==OrganizationStatus.DELETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Use DELETE to soft-delete an organization");
        }

        organization.setStatus(status);
        organization.setUpdatedAt(LocalDateTime.now());

        return organization;
    }

}
