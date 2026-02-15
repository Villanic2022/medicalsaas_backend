package com.saas.medical.repository;

import com.saas.medical.model.entity.PatientFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientFileRepository extends JpaRepository<PatientFile, Long> {

    @Query("SELECT pf FROM PatientFile pf WHERE pf.patient.id = :patientId AND pf.tenantId = :tenantId AND pf.active = true ORDER BY pf.createdAt DESC")
    List<PatientFile> findByPatientIdAndTenantIdOrderByCreatedAtDesc(Long patientId, UUID tenantId);

    @Query("SELECT pf FROM PatientFile pf WHERE pf.id = :id AND pf.tenantId = :tenantId AND pf.active = true")
    Optional<PatientFile> findByIdAndTenantId(Long id, UUID tenantId);

    @Query("SELECT pf FROM PatientFile pf WHERE pf.id = :id AND pf.patient.id = :patientId AND pf.tenantId = :tenantId AND pf.active = true")
    Optional<PatientFile> findByIdAndPatientIdAndTenantId(Long id, Long patientId, UUID tenantId);
}

