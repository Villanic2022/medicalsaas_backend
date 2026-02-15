package com.saas.medical.repository;

import com.saas.medical.model.entity.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId AND p.dni = :dni AND p.active = true")
    Optional<Patient> findByTenantIdAndDni(UUID tenantId, String dni);

    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId AND p.active = true")
    List<Patient> findByTenantIdAndActive(UUID tenantId);

    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId AND p.id = :id AND p.active = true")
    Optional<Patient> findByTenantIdAndId(UUID tenantId, Long id);

    @Query("SELECT p FROM Patient p JOIN FETCH p.tenant WHERE p.id = :id AND p.active = true")
    Optional<Patient> findByIdWithTenant(Long id);

    // Query nativa de PostgreSQL para evitar problemas con tipos BYTEA
    @Query(value = "SELECT p.* FROM patients p " +
           "LEFT JOIN insurance_companies ic ON ic.id = p.insurance_company_id " +
           "LEFT JOIN professionals pp ON pp.id = p.preferred_professional_id " +
           "WHERE p.tenant_id = :tenantId " +
           "AND (:search IS NULL OR " +
           "LOWER(p.first_name || ' ' || p.last_name) LIKE LOWER('%' || :search || '%') OR " +
           "CAST(p.dni AS text) LIKE '%' || :search || '%') " + 
           "AND (:insuranceId IS NULL OR p.insurance_company_id = :insuranceId) " +
           "AND (:professionalId IS NULL OR p.preferred_professional_id = :professionalId) " +
           "AND p.active = true " +
           "ORDER BY p.first_name, p.last_name", 
           nativeQuery = true)
    List<Patient> findPatientsWithFilters(@Param("tenantId") UUID tenantId,
                                        @Param("search") String search,
                                        @Param("insuranceId") Long insuranceId,
                                        @Param("professionalId") Long professionalId);

    // Query nativa de PostgreSQL para búsqueda
    @Query(value = "SELECT p.* FROM patients p " +
           "WHERE p.tenant_id = :tenantId " +
           "AND (LOWER(p.first_name || ' ' || p.last_name) LIKE LOWER('%' || :query || '%') OR " +
           "CAST(p.dni AS text) LIKE '%' || :query || '%') " +
           "AND p.active = true " +
           "ORDER BY p.first_name, p.last_name " +
           "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}", 
           nativeQuery = true)
    List<Patient> searchPatients(@Param("tenantId") UUID tenantId, 
                               @Param("query") String query, 
                               Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM Patient p WHERE p.tenant.id = :tenantId AND p.dni = :dni AND p.id != :excludeId")
    boolean existsByTenantIdAndDniAndIdNot(UUID tenantId, String dni, Long excludeId);

    @Query("SELECT COUNT(p) > 0 FROM Patient p WHERE p.tenant.id = :tenantId AND p.dni = :dni")
    boolean existsByTenantIdAndDni(UUID tenantId, String dni);

    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId AND p.dni = :dni AND p.active = false")
    Optional<Patient> findInactiveByTenantIdAndDni(UUID tenantId, String dni);
}