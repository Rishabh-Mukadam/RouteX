package com.ridam.routex.management.service;

import com.ridam.routex.management.dto.requestDto.OrganizationRequest;
import com.ridam.routex.management.dto.requestDto.TenantAdminRequest;
import com.ridam.routex.management.dto.requestDto.TenantOrganizationRequest;
import com.ridam.routex.management.dto.responseDto.OrganizationResponse;
import com.ridam.routex.management.dto.responseDto.TenantAdminResponse;
import com.ridam.routex.management.entity.ManagementUser;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import com.ridam.routex.management.entity.Role;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.ManagementUserRepository;
import com.ridam.routex.management.repository.OrganizationRepository;
import com.ridam.routex.management.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private ManagementUserRepository managementUserRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final TenantContext context;


    public OrganizationService(
            OrganizationRepository repository,
            ManagementUserRepository managementUserRepository,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            TenantContext context){
        this.repository=repository;
        this.managementUserRepository=managementUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.context = context;
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
        if(org.getStatus()==OrganizationStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.ALREADY_REPORTED,
                    "Organization is already deleted from the database"
            );
        }
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

//        if(current.getStatus()==OrganizationStatus.DELETED) {
//            throw new ApiException(
//                    HttpStatus.ALREADY_REPORTED,
//                    "Organization is already deleted from the database"
//            );
//        }

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



//        if(organization.getStatus()==OrganizationStatus.DELETED){
//            throw new ApiException(
//                    HttpStatus.ALREADY_REPORTED,
//                    "Organization is already deleted from the database"
//            );
//        }

        if(status==OrganizationStatus.DELETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Use DELETE to soft-delete an organization");
        }

        organization.setStatus(status);
        organization.setUpdatedAt(LocalDateTime.now());

        return organization;
    }

    @Transactional
    public Organization deleteOrg(Long id) {
        Organization organization=repository.findById(id).orElseThrow(
                ()->new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Organization not exists with id : "+id)
        );


        if(organization.getStatus()==OrganizationStatus.DELETED) {
            throw new ApiException(
                    HttpStatus.ALREADY_REPORTED,
                    "Organization is already deleted from the database"
            );
        }
        organization.setStatus(OrganizationStatus.DELETED);
        return organization;
    }


//    tenant service



    @Transactional
    public TenantAdminResponse createTenantAdmin(Long organizationId, TenantAdminRequest request) {
        long startedAt = System.nanoTime();
        Organization organization = getOrganization(organizationId);

        if(managementUserRepository.findByOrganizationId(organizationId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This organization already has a tenant administrator");
        }

        if(managementUserRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        Role role = roleRepository.findByName("TENANT_ADMIN")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "TENANT_ADMIN role is missing"));

        ManagementUser user = new ManagementUser();
        user.setOrganization(organization);
        user.setRole(role);
        user.setUsername(request.getUsername());
        user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());
        user.setCreated_at(LocalDateTime.now());
        user.setUpdated_at(LocalDateTime.now());
        managementUserRepository.save(user);

        return TenantAdminResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TenantAdminResponse getTenantAdmin(Long organizationId) {
        return TenantAdminResponse.from(getTenantAdminEntity(organizationId));
    }

    @Transactional
    public TenantAdminResponse updateTenantAdmin(Long organizationId, TenantAdminRequest request) {
        long startedAt = System.nanoTime();
        ManagementUser user = getTenantAdminEntity(organizationId);

        if(!user.getUsername().equals(request.getUsername())
                && managementUserRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        user.setUsername(request.getUsername());
        user.setPassword_hash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());
        user.setUpdated_at(LocalDateTime.now());

        return TenantAdminResponse.from(user);
    }

    @Transactional
    public void deleteTenantAdmin(Long organizationId) {
        long startedAt = System.nanoTime();

        ManagementUser user = getTenantAdminEntity(organizationId);

        managementUserRepository.delete(user);
    }

    private ManagementUser getTenantAdminEntity(Long organizationId) {
        getOrganization(organizationId);
        return managementUserRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant administrator not found"));
    }


    private Organization getOrganization(Long id) {
        return repository.findByIdAndStatusNot(id, OrganizationStatus.DELETED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
    }


    public OrganizationResponse getOwnOrganization() {
        Organization organization=context.getOrganization();
        return OrganizationResponse.from(organization);
    }

    public OrganizationResponse updateOwnOrganization(TenantOrganizationRequest request) {
        Organization organization=context.getOrganization();

        if(!organization.getName().equalsIgnoreCase(request.getName())
                && repository.existsByNameIgnoreCase(request.getName())){
            throw new ApiException(HttpStatus.CONFLICT, "Organization name already exists");
        }
        organization.setName(request.getName());
        organization.setUpdatedAt(LocalDateTime.now());

        return OrganizationResponse.from(organization);
    }
}
