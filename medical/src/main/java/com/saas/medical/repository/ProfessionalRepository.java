package com.saas.medical.repository;

import com.saas.medical.model.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findByTenantIdAndActive(UUID tenantId, Boolean active);

    @Query("SELECT DISTINCT p FROM Professional p " +
           "JOIN FETCH p.specialty " +
           "LEFT JOIN FETCH p.acceptedInsurances " +
           "WHERE p.tenantId = :tenantId AND p.active = true")
    List<Professional> findByTenantIdWithSpecialty(UUID tenantId);

    @Query("SELECT DISTINCT p FROM Professional p " +
           "JOIN FETCH p.specialty " +
           "LEFT JOIN FETCH p.acceptedInsurances " +
           "WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<Professional> findByIdAndTenantId(Long id, UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    boolean existsByLicenseNumberAndTenantId(String licenseNumber, UUID tenantId);

    @Query("SELECT p FROM Professional p WHERE p.email = :email AND p.tenantId = :tenantId AND p.active = false")
    Optional<Professional> findInactiveByEmailAndTenantId(String email, UUID tenantId);

    @Query("SELECT p FROM Professional p WHERE p.licenseNumber = :licenseNumber AND p.tenantId = :tenantId AND p.active = false")
    Optional<Professional> findInactiveByLicenseNumberAndTenantId(String licenseNumber, UUID tenantId);

    @Query("SELECT p FROM Professional p WHERE p.email = :email AND p.tenantId = :tenantId AND p.active = true")
    Optional<Professional> findActiveByEmailAndTenantId(String email, UUID tenantId);
}
