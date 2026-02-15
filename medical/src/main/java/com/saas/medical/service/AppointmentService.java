package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.appointment.AppointmentRequest;
import com.saas.medical.model.dto.appointment.AppointmentResponse;
import com.saas.medical.model.entity.Appointment;
import com.saas.medical.model.entity.Patient;
import com.saas.medical.model.entity.Professional;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.repository.AppointmentRepository;
import com.saas.medical.repository.PatientRepository;
import com.saas.medical.repository.ProfessionalRepository;
import com.saas.medical.repository.TenantRepository;
import com.saas.medical.repository.UserRepository;
import com.saas.medical.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public AppointmentResponse createAppointment(String tenantSlug, AppointmentRequest request) {
        // Buscar tenant por slug
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + tenantSlug));

        // Buscar profesional
        Professional professional = professionalRepository.findByIdAndTenantId(request.getProfessionalId(), tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        // Verificar que el horario esté disponible
        LocalDateTime appointmentDateTime = request.getStartDateTime();
        if (appointmentRepository.existsAppointmentAtTime(professional.getId(), appointmentDateTime)) {
            throw new BusinessException("El horario seleccionado no está disponible");
        }

        // Verificar que la fecha no sea en el pasado
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("No se puede reservar un turno en el pasado");
        }

        // Buscar o crear paciente
        Patient patient = findOrCreatePatient(tenant.getId(), request);

        // Calcular hora de fin (duración por defecto del tenant)
        LocalDateTime endDateTime = appointmentDateTime.plusMinutes(tenant.getAppointmentDurationMinutes());

        // Crear appointment
        Appointment appointment = new Appointment();
        appointment.setTenantId(tenant.getId());
        appointment.setProfessional(professional);
        appointment.setPatient(patient);
        appointment.setStartDateTime(appointmentDateTime);
        appointment.setEndDateTime(endDateTime);
        appointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        appointment.setNotes(request.getNotes());

        appointment = appointmentRepository.save(appointment);

        log.info("Turno creado: {} - {} {} con Dr. {} {}",
                appointmentDateTime,
                patient.getFirstName(), patient.getLastName(),
                professional.getFirstName(), professional.getLastName());

        // Enviar email de confirmación
        try {
            emailService.sendAppointmentConfirmation(appointment);
        } catch (Exception e) {
            log.error("Error enviando email de confirmación: {}", e.getMessage());
        }

        return mapToResponse(appointment, tenant);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAppointmentsByTenant(UUID tenantId) {
        List<Appointment> appointments = appointmentRepository.findByTenantId(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        return appointments.stream()
                .map(appointment -> mapToResponse(appointment, tenant))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAppointmentsByCurrentTenant() {
        UUID tenantId = getCurrentTenantId();

        // Verificar si el usuario es PROFESSIONAL para filtrar solo sus turnos
        List<Appointment> appointments;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isProfessional = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_PROFESSIONAL"));

        if (isProfessional) {
            // Obtener el professionalId del usuario autenticado
            String email = authentication.getName();
            Long professionalId = getProfessionalIdByEmail(email);

            if (professionalId != null) {
                log.info("Usuario PROFESSIONAL - filtrando turnos para professionalId: {}", professionalId);
                appointments = appointmentRepository.findByTenantIdAndProfessionalId(tenantId, professionalId);
            } else {
                log.warn("Usuario PROFESSIONAL {} no tiene professional vinculado, retornando lista vacía", email);
                return List.of();
            }
        } else {
            appointments = appointmentRepository.findByTenantIdOrderByStartDateTimeDesc(tenantId);
        }

        if (appointments.isEmpty()) {
            return List.of();
        }
        
        // Obtener tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        return appointments.stream()
                .map(appointment -> mapToResponse(appointment, tenant))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el professionalId asociado al email del usuario
     */
    private Long getProfessionalIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMap(user -> professionalRepository.findByUserId(user.getId()))
                .map(Professional::getId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findByIdAndCurrentTenant(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
        
        Tenant tenant = tenantRepository.findById(appointment.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        
        // TODO: Verificar que el appointment pertenece al tenant actual del usuario
        
        return mapToResponse(appointment, tenant);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
        
        // TODO: Verificar que el appointment pertenece al tenant actual del usuario
        
        try {
            Appointment.AppointmentStatus newStatus = Appointment.AppointmentStatus.valueOf(status.toUpperCase());
            appointment.setStatus(newStatus);
            appointment = appointmentRepository.save(appointment);
            
            log.info("Estado de turno actualizado - ID: {}, Nuevo estado: {}", id, newStatus);
            
            Tenant tenant = tenantRepository.findById(appointment.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
                    
            return mapToResponse(appointment, tenant);
            
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado inválido: " + status + ". Estados válidos: CONFIRMED, CANCELLED, COMPLETED");
        }
    }

    @Transactional
    public void cancel(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
        
        // TODO: Verificar que el appointment pertenece al tenant actual del usuario
        
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        
        log.info("Turno cancelado - ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findConfirmedAppointments(String tenantSlug, Long professionalId, LocalDateTime date) {
        // Buscar tenant
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + tenantSlug));

        // Buscar profesional
        Professional professional = professionalRepository.findByIdAndTenantId(professionalId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));

        // Obtener turnos ocupados para el día
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        List<Appointment> existingAppointments = appointmentRepository
                .findByProfessionalAndDateRange(professionalId, startOfDay, endOfDay);

        // Devolver las citas confirmadas (para que el frontend sepa qué horarios están ocupados--perfecto para mostrar en el calendario)
        return existingAppointments.stream()
                .filter(appointment -> appointment.getStatus() == Appointment.AppointmentStatus.CONFIRMED)
                .map(appointment -> mapToResponse(appointment, tenant))
                .collect(Collectors.toList());
    }

    private Patient findOrCreatePatient(UUID tenantId, AppointmentRequest request) {
        // Buscar paciente existente por DNI
        return patientRepository.findByTenantIdAndDni(tenantId, request.getPatient().getDni())
                .orElseGet(() -> {
                    // Crear nuevo paciente
                    Patient newPatient = new Patient();
                    // Buscar tenant para la nueva estructura
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));
                    newPatient.setTenant(tenant);
                    newPatient.setDni(request.getPatient().getDni());
                    newPatient.setFirstName(request.getPatient().getFirstName());
                    newPatient.setLastName(request.getPatient().getLastName());
                    newPatient.setEmail(request.getPatient().getEmail());
                    newPatient.setPhone(request.getPatient().getPhone());
                    // Para compatibilidad con la API antigua, usar un valor por defecto
                    newPatient.setInsuranceNumber(request.getPatient().getInsuranceNumber());
                    newPatient.setActive(true);

                    return patientRepository.save(newPatient);
                });
    }

    private AppointmentResponse mapToResponse(Appointment appointment, Tenant tenant) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .startDateTime(appointment.getStartDateTime())
                .endDateTime(appointment.getEndDateTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .professional(AppointmentResponse.ProfessionalInfo.builder()
                        .id(appointment.getProfessional().getId())
                        .firstName(appointment.getProfessional().getFirstName())
                        .lastName(appointment.getProfessional().getLastName())
                        .fullName(appointment.getProfessional().getFullName())
                        .specialtyName(appointment.getProfessional().getSpecialty().getName())
                        .build())
                .patient(AppointmentResponse.PatientInfo.builder()
                        .id(appointment.getPatient().getId())
                        .dni(appointment.getPatient().getDni())
                        .firstName(appointment.getPatient().getFirstName())
                        .lastName(appointment.getPatient().getLastName())
                        .fullName(appointment.getPatient().getFullName())
                        .email(appointment.getPatient().getEmail())
                        .phone(appointment.getPatient().getPhone())

                        .insuranceNumber(appointment.getPatient().getInsuranceNumber())
                        .build())
                .build();
    }

    private UUID getCurrentTenantId() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new BusinessException("No se pudo determinar el tenant actual. Verifique que esté autenticado correctamente.");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("ID de tenant inválido: " + tenantId);
        }
    }
}
