package com.ridam.routex.management.controller;


import com.ridam.routex.management.dto.requestDto.OrganizationRequest;
import com.ridam.routex.management.dto.requestDto.TenantAdminRequest;
import com.ridam.routex.management.dto.responseDto.OrganizationResponse;
import com.ridam.routex.management.dto.responseDto.TenantAdminResponse;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.OrganizationRepository;
import com.ridam.routex.management.service.OrganizationService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/organizations")
public class PlatformOrganizationController {

//    private final RepositoryMethodInvocationListener repositoryMethodInvocationListener;
    private OrganizationService organizationService;


    public PlatformOrganizationController(OrganizationService organizationService, OrganizationRepository organizationRepository){
        this.organizationService=organizationService;
//        this.repositoryMethodInvocationListener = repositoryMethodInvocationListener;
    }

//    1.Create/Onboard new Organization

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request){
        Organization org= organizationService.create(request);
        return ResponseEntity.ok(OrganizationResponse.from(org));
    }

//    2 read all
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> findAll(){
        List<OrganizationResponse> list= organizationService.findAll();
        return ResponseEntity.ok(list);
    }

//    3 read one

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> findById(@PathVariable Long id){
        return ResponseEntity.ok(organizationService.findById(id));
    }

//    4 update
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(@PathVariable Long id,@Valid @RequestBody OrganizationRequest request){
        Organization org= organizationService.update(id,request);
        return ResponseEntity.ok(OrganizationResponse.from(org));
    }
//
//
////    5 patch
    @PatchMapping("/{id}/{status}")
    public ResponseEntity<OrganizationResponse> update(@PathVariable Long id,@RequestBody Map<String,String> request){
        OrganizationStatus status1=OrganizationStatus.valueOf(request.get("status"));
        Organization org= organizationService.changeStatus(id,status1);
        return ResponseEntity.ok(OrganizationResponse.from(org));
    }

//
//
////    6 soft delet
    @DeleteMapping("/{id}")
    public ResponseEntity<OrganizationResponse> Delete(@PathVariable Long id){
        Organization org= organizationService.deleteOrg(id);
        return ResponseEntity.ok(OrganizationResponse.from(org));
    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id){
//        organizationService.delete(id);
//        return ResponseEntity.noContent().build();
//    }




    // CRUD --> Tenant Admins
    @PostMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> createTenantAdmin(@PathVariable Long organizationId,
                                                                 @Valid @RequestBody TenantAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createTenantAdmin(organizationId, request));
    }

    @GetMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> getTenantAdmin(@PathVariable Long organizationId) {
        return ResponseEntity.ok(organizationService.getTenantAdmin(organizationId));
    }

    @PutMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<TenantAdminResponse> updateTenantAdmin(@PathVariable Long organizationId,
                                                                 @Valid @RequestBody TenantAdminRequest request) {
        return ResponseEntity.ok(organizationService.updateTenantAdmin(organizationId, request));
    }

    @DeleteMapping("/{organizationId}/tenant-admin")
    public ResponseEntity<Void> deleteTenantAdmin(@PathVariable Long organizationId) {
        organizationService.deleteTenantAdmin(organizationId);
        return ResponseEntity.noContent().build();
    }
}




