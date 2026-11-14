package com.ridam.routex.management.controller;


import com.ridam.routex.management.dto.requestDto.OrganizationRequest;
import com.ridam.routex.management.dto.responseDto.OrganizationResponse;
import com.ridam.routex.management.entity.Organization;
import com.ridam.routex.management.entity.OrganizationStatus;
import com.ridam.routex.management.exception.ApiException;
import com.ridam.routex.management.repository.OrganizationRepository;
import com.ridam.routex.management.service.OrganizationService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/organization")
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
    @GetMapping("/getAll")
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
    public ResponseEntity<OrganizationResponse> update(@PathVariable Long id,@PathVariable OrganizationStatus status){
        Organization org= organizationService.changeStatus(id,status);
        return ResponseEntity.ok(OrganizationResponse.from(org));
    }

//
//
////    6 soft delet
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id){
//        organizationService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}




