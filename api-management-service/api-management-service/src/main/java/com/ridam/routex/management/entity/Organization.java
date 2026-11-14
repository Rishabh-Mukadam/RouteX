package com.ridam.routex.management.entity;

import com.ridam.routex.management.dto.requestDto.OrganizationRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@NoArgsConstructor
@Getter
@Setter
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String slug;

    @Enumerated(EnumType.STRING)
    private OrganizationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Organization from(OrganizationRequest request){
        Organization new_org=new Organization();
        new_org.setName(request.getName());
        new_org.setSlug(request.getSlug());
        new_org.setStatus(OrganizationStatus.PENDING);
        new_org.setCreatedAt(LocalDateTime.now());
        new_org.setUpdatedAt(LocalDateTime.now());

        return new_org;
    }

}
