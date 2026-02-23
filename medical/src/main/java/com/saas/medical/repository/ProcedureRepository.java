package com.saas.medical.repository;

import com.saas.medical.model.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {

    @Query("SELECT p FROM Procedure p WHERE p.tenantId = :tenantId AND p.active = true ORDER BY p.name")
    List<Procedure> findByTenantIdAndActiveTrue(UUID tenantId);

    @Query("SELECT p FROM Procedure p WHERE p.tenantId = :tenantId ORDER BY p.name")
    List<Procedure> findByTenantId(UUID tenantId);

    Optional<Procedure> findByIdAndTenantId(Long id, UUID tenantId);

    @Query("SELECT p FROM Procedure p WHERE p.tenantId = :tenantId AND p.specialty.id = :specialtyId AND p.active = true ORDER BY p.name")
    List<Procedure> findByTenantIdAndSpecialtyIdAndActiveTrue(UUID tenantId, Long specialtyId);

    @Query("SELECT p FROM Procedure p WHERE p.tenantId = :tenantId AND p.name = :name")
    Optional<Procedure> findByTenantIdAndName(UUID tenantId, String name);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Procedure p WHERE p.tenantId = :tenantId AND p.name = :name")
    boolean existsByTenantIdAndName(UUID tenantId, String name);
}

