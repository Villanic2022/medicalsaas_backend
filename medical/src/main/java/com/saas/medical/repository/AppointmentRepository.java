package com.saas.medical.repository;

import com.saas.medical.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId ORDER BY a.startDateTime")
    List<Appointment> findByTenantId(UUID tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId ORDER BY a.startDateTime DESC")
    List<Appointment> findByTenantIdOrderByStartDateTimeDesc(UUID tenantId);

    @Query("SELECT a FROM Appointment a WHERE a.professional.id = :professionalId " +
           "AND a.startDateTime >= :fromDateTime AND a.startDateTime <= :toDateTime " +
           "AND a.status != 'CANCELLED' ORDER BY a.startDateTime")
    List<Appointment> findByProfessionalAndDateRange(Long professionalId, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId ORDER BY a.startDateTime DESC")
    List<Appointment> findByPatientId(Long patientId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.professional.id = :professionalId " +
           "AND a.startDateTime = :startDateTime AND a.status != 'CANCELLED'")
    boolean existsAppointmentAtTime(Long professionalId, LocalDateTime startDateTime);

    @Query("SELECT a FROM Appointment a WHERE a.tenantId = :tenantId " +
           "AND a.startDateTime >= :fromDate AND a.startDateTime <= :toDate " +
           "ORDER BY a.startDateTime")
    List<Appointment> findByTenantIdAndDateRange(UUID tenantId, LocalDateTime fromDate, LocalDateTime toDate);
}
