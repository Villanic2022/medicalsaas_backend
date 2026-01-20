package com.saas.medical.repository;

import com.saas.medical.model.entity.ProfessionalAvailability;
import com.saas.medical.model.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalAvailabilityRepository extends JpaRepository<ProfessionalAvailability, Long> {

    List<ProfessionalAvailability> findByProfessionalIdAndActiveTrue(Long professionalId);

    @Query("SELECT pa FROM ProfessionalAvailability pa WHERE pa.professional.id = :professionalId " +
           "AND pa.dayOfWeek = :dayOfWeek AND pa.active = true AND pa.specificDate IS NULL")
    List<ProfessionalAvailability> findByProfessionalIdAndDayOfWeekAndActive(
            @Param("professionalId") Long professionalId, 
            @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT pa FROM ProfessionalAvailability pa WHERE pa.professional.id = :professionalId " +
           "AND pa.specificDate = :specificDate AND pa.active = true")
    List<ProfessionalAvailability> findByProfessionalIdAndSpecificDateAndActive(
            @Param("professionalId") Long professionalId, 
            @Param("specificDate") LocalDate specificDate);

    @Query("SELECT pa FROM ProfessionalAvailability pa " +
           "JOIN pa.professional p " +
           "WHERE p.tenantId = :tenantId AND pa.active = true")
    List<ProfessionalAvailability> findByTenantIdAndActive(@Param("tenantId") java.util.UUID tenantId);

    @Modifying
    @Transactional
    void deleteByProfessionalId(Long professionalId);

    @Query("SELECT pa FROM ProfessionalAvailability pa " +
           "JOIN pa.professional p " +
           "WHERE pa.id = :id AND p.tenantId = :tenantId")
    Optional<ProfessionalAvailability> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") java.util.UUID tenantId);
}
