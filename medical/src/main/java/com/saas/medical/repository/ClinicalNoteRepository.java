package com.saas.medical.repository;

import com.saas.medical.model.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {

    @Query("SELECT cn FROM ClinicalNote cn " +
           "JOIN FETCH cn.patient " +
           "JOIN FETCH cn.professional " +
           "WHERE cn.patient.id = :patientId AND cn.tenantId = :tenantId AND cn.active = true " +
           "ORDER BY cn.createdAt DESC")
    List<ClinicalNote> findByPatientIdAndTenantIdOrderByCreatedAtDesc(Long patientId, UUID tenantId);

    @Query("SELECT cn FROM ClinicalNote cn " +
           "JOIN FETCH cn.patient " +
           "JOIN FETCH cn.professional " +
           "WHERE cn.id = :id AND cn.tenantId = :tenantId AND cn.active = true")
    Optional<ClinicalNote> findByIdAndTenantId(Long id, UUID tenantId);

    @Query("SELECT cn FROM ClinicalNote cn " +
           "JOIN FETCH cn.patient " +
           "JOIN FETCH cn.professional " +
           "WHERE cn.id = :id AND cn.patient.id = :patientId AND cn.tenantId = :tenantId AND cn.active = true")
    Optional<ClinicalNote> findByIdAndPatientIdAndTenantId(Long id, Long patientId, UUID tenantId);
}

