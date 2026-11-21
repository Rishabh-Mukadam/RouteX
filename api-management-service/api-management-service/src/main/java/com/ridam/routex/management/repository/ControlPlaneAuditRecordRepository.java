package com.ridam.routex.management.repository;

import com.ridam.routex.management.entity.ControlPlaneAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlPlaneAuditRecordRepository extends JpaRepository<ControlPlaneAuditRecord,Long> {
    List<ControlPlaneAuditRecord> findAllByOrganizationIdOrderByCreatedAtDesc(Long id);
}
