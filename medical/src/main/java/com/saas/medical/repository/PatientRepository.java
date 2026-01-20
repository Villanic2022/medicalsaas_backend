package com.saas.medical.repository;

import com.saas.medical.model.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.dni = :dni")
    Optional<Patient> findByTenantIdAndDni(UUID tenantId, String dni);

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.email = :email")
    Optional<Patient> findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId")
    List<Patient> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndDni(UUID tenantId, String dni);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
}
