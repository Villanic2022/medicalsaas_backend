package com.saas.medical.controller;

import com.saas.medical.model.dto.professional.ProfessionalRequest;
import com.saas.medical.model.dto.professional.ProfessionalResponse;
import com.saas.medical.model.dto.professional.ProfessionalAvailabilityRequest;
import com.saas.medical.model.dto.professional.ProfessionalAvailabilityResponse;
import com.saas.medical.security.TenantContext;
import com.saas.medical.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@Tag(name = "Profesionales", description = "Gestión de profesionales del consultorio")
@SecurityRequirement(name = "bearerAuth")

public class ProfessionalController {

    private final ProfessionalService professionalService;

    @GetMapping
    @Operation(summary = "Listar profesionales", description = "Obtiene todos los profesionales del consultorio")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<List<ProfessionalResponse>> findAll() {
        List<ProfessionalResponse> professionals = professionalService.findAllByTenant();
        return ResponseEntity.ok(professionals);
    }

    @GetMapping("/debug")
    @Operation(summary = "Debug info", description = "Información de debugging")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<Map<String, Object>> debugInfo() {
        Map<String, Object> debug = new HashMap<>();
        try {
            debug.put("authenticationExists", SecurityContextHolder.getContext().getAuthentication() != null);
            debug.put("authenticationName", SecurityContextHolder.getContext().getAuthentication().getName());
            debug.put("tenantContext", TenantContext.getCurrentTenant());
            debug.put("authorities", SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        } catch (Exception e) {
            debug.put("error", e.getMessage());
        }
        return ResponseEntity.ok(debug);
    }

    @GetMapping("/debug-no-auth")
    @Operation(summary = "Debug sin auth", description = "Ver profesionales sin autenticación")
    public ResponseEntity<String> debugNoAuth() {
        return ResponseEntity.ok("Backend funcionando - timestamp: " + System.currentTimeMillis());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener profesional", description = "Obtiene un profesional por ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<ProfessionalResponse> findById(@PathVariable Long id) {
        ProfessionalResponse professional = professionalService.findById(id);
        return ResponseEntity.ok(professional);
    }

    @PostMapping
    @Operation(summary = "Crear profesional", description = "Crea un nuevo profesional")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse professional = professionalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(professional);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar profesional", description = "Actualiza un profesional existente")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ProfessionalResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse professional = professionalService.update(id, request);
        return ResponseEntity.ok(professional);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar profesional", description = "Desactiva un profesional")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professionalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ENDPOINTS DE DISPONIBILIDAD ====================

    @GetMapping("/{id}/availability")
    @Operation(summary = "Obtener disponibilidad", description = "Obtiene la configuración de horarios de un profesional")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PROFESSIONAL')")
    public ResponseEntity<List<ProfessionalAvailabilityResponse>> getAvailability(@PathVariable Long id) {
        List<ProfessionalAvailabilityResponse> availability = professionalService.getAvailability(id);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{id}/availability/date/{date}")
    @Operation(summary = "Obtener disponibilidad para fecha específica", 
               description = "Obtiene la disponibilidad para una fecha específica, priorizando configuraciones específicas sobre recurrentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'PROFESSIONAL')")
    public ResponseEntity<List<ProfessionalAvailabilityResponse>> getAvailabilityForDate(
            @PathVariable Long id,
            @PathVariable LocalDate date) {
        List<ProfessionalAvailabilityResponse> availability = professionalService.getAvailabilityForDate(id, date);
        return ResponseEntity.ok(availability);
    }

    @PostMapping("/{id}/availability")
    @Operation(summary = "Agregar disponibilidad", description = "Agrega una nueva configuración de horario para un profesional")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ProfessionalAvailabilityResponse> addAvailability(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalAvailabilityRequest request) {
        ProfessionalAvailabilityResponse availability = professionalService.addAvailability(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(availability);
    }

    @PutMapping("/{id}/availability")
    @Operation(summary = "Actualizar disponibilidad", description = "Reemplaza completamente la configuración de horarios de un profesional")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<List<ProfessionalAvailabilityResponse>> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody List<ProfessionalAvailabilityRequest> requests) {
        List<ProfessionalAvailabilityResponse> availability = professionalService.updateAvailability(id, requests);
        return ResponseEntity.ok(availability);
    }

    @DeleteMapping("/availability/{availabilityId}")
    @Operation(summary = "Eliminar disponibilidad", description = "Elimina una configuración específica de horario")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long availabilityId) {
        professionalService.deleteAvailability(availabilityId);
        return ResponseEntity.noContent().build();
    }
}
