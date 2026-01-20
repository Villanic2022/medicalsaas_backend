package com.saas.medical.controller;

import com.saas.medical.model.dto.appointment.AppointmentRequest;
import com.saas.medical.model.dto.appointment.AppointmentResponse;
import com.saas.medical.model.dto.appointment.AppointmentConfirmationResponse;
import com.saas.medical.model.dto.professional.ProfessionalResponse;
import com.saas.medical.model.dto.professional.ProfessionalAvailabilityResponse;
import com.saas.medical.model.dto.specialty.SpecialtyResponse;
import com.saas.medical.model.dto.tenant.TenantResponse;
import com.saas.medical.service.AppointmentService;
import com.saas.medical.service.ProfessionalService;
import com.saas.medical.service.SpecialtyService;
import com.saas.medical.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/t/{tenantSlug}")
@RequiredArgsConstructor
@Tag(name = "Turnos Públicos", description = "API pública para reserva de turnos médicos")
public class PublicAppointmentController {

    private final AppointmentService appointmentService;
    private final ProfessionalService professionalService;
    private final SpecialtyService specialtyService;
    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "Información del consultorio", description = "Obtiene información básica del consultorio")
    public ResponseEntity<TenantResponse> getTenantInfo(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug) {
        TenantResponse tenant = tenantService.findBySlug(tenantSlug);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/professionals")
    @Operation(summary = "Obtener profesionales del consultorio",
               description = "Lista todos los profesionales activos del consultorio")
    public ResponseEntity<List<ProfessionalResponse>> getProfessionals(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug) {
        List<ProfessionalResponse> professionals = professionalService.findAllByTenantSlug(tenantSlug);
        return ResponseEntity.ok(professionals);
    }

    @GetMapping("/professionals/{professionalId}/availability")
    @Operation(summary = "Obtener disponibilidad de un profesional",
               description = "Obtiene la configuración de horarios activos de un profesional (endpoint público)")
    public ResponseEntity<List<ProfessionalAvailabilityResponse>> getProfessionalAvailability(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug,
            @Parameter(description = "ID del profesional") @PathVariable Long professionalId) {
        List<ProfessionalAvailabilityResponse> availability = 
                professionalService.getAvailabilityByTenantSlug(tenantSlug, professionalId);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/specialties")
    @Operation(summary = "Obtener especialidades",
               description = "Lista todas las especialidades disponibles")
    public ResponseEntity<List<SpecialtyResponse>> getSpecialties(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug) {
        List<SpecialtyResponse> specialties = specialtyService.findAllActive();
        return ResponseEntity.ok(specialties);
    }

    @GetMapping("/appointments")
    @Operation(summary = "Obtener citas confirmadas",
               description = "Obtiene las citas confirmadas de un profesional para una fecha específica (para mostrar horarios ocupados)")
    public ResponseEntity<List<AppointmentResponse>> getAvailableSlots(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug,
            @Parameter(description = "ID del profesional") @RequestParam Long professionalId,
            @Parameter(description = "Fecha (formato: YYYY-MM-DD)") @RequestParam String date) {
        // Convertir string a LocalDateTime (al inicio del día)
        LocalDateTime dateTime = LocalDate.parse(date).atStartOfDay();
        List<AppointmentResponse> confirmedAppointments =
                appointmentService.findConfirmedAppointments(tenantSlug, professionalId, dateTime);
        return ResponseEntity.ok(confirmedAppointments);
    }

    @PostMapping("/appointments")
    @Operation(summary = "Crear turno",
               description = "Crea un nuevo turno médico para el paciente")
    public ResponseEntity<AppointmentConfirmationResponse> createAppointment(
            @Parameter(description = "Slug del consultorio") @PathVariable String tenantSlug,
            @Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.createAppointment(tenantSlug, request);
        AppointmentConfirmationResponse confirmation = AppointmentConfirmationResponse.from(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
    }

}
