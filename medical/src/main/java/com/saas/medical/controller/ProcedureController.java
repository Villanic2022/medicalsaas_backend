package com.saas.medical.controller;

import com.saas.medical.model.dto.procedure.ProcedureRequest;
import com.saas.medical.model.dto.procedure.ProcedureResponse;
import com.saas.medical.service.ProcedureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/procedures")
@RequiredArgsConstructor
@Tag(name = "Procedimientos", description = "APIs para gestión de procedimientos médicos")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ProcedureController {

    private final ProcedureService procedureService;

    @GetMapping
    @Operation(summary = "Listar procedimientos", description = "Lista todos los procedimientos activos del tenant")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<List<ProcedureResponse>> getAllProcedures(
            @Parameter(description = "Incluir procedimientos inactivos")
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        try {
            log.info("=== GET /procedures llamado, includeInactive={} ===", includeInactive);
            List<ProcedureResponse> procedures = includeInactive
                    ? procedureService.findAllByCurrentTenantIncludingInactive()
                    : procedureService.findAllByCurrentTenant();
            log.info("=== Procedimientos encontrados: {} ===", procedures.size());
            return ResponseEntity.ok(procedures);
        } catch (Exception e) {
            log.error("=== ERROR en GET /procedures: {} ===", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener procedimiento por ID", description = "Obtiene los detalles de un procedimiento específico")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<ProcedureResponse> getProcedure(@PathVariable Long id) {
        ProcedureResponse procedure = procedureService.findById(id);
        return ResponseEntity.ok(procedure);
    }

    @GetMapping("/by-specialty/{specialtyId}")
    @Operation(summary = "Listar procedimientos por especialidad",
               description = "Lista todos los procedimientos activos de una especialidad específica")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<List<ProcedureResponse>> getProceduresBySpecialty(@PathVariable Long specialtyId) {
        List<ProcedureResponse> procedures = procedureService.findBySpecialty(specialtyId);
        return ResponseEntity.ok(procedures);
    }

    @PostMapping
    @Operation(summary = "Crear procedimiento", description = "Crea un nuevo procedimiento médico")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<ProcedureResponse> createProcedure(@Valid @RequestBody ProcedureRequest request) {
        ProcedureResponse procedure = procedureService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(procedure);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar procedimiento", description = "Actualiza un procedimiento existente")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<ProcedureResponse> updateProcedure(
            @PathVariable Long id,
            @Valid @RequestBody ProcedureRequest request) {
        ProcedureResponse procedure = procedureService.update(id, request);
        return ResponseEntity.ok(procedure);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar procedimiento", description = "Elimina (desactiva) un procedimiento")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<Void> deleteProcedure(@PathVariable Long id) {
        procedureService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/template-load")
    @Operation(summary = "Cargar plantilla de procedimientos",
               description = "Carga procedimientos comunes para una especialidad específica. " +
                       "Especialidades disponibles: DENTISTRY, GENERAL_MEDICINE, DERMATOLOGY, CARDIOLOGY, " +
                       "GYNECOLOGY, PEDIATRICS, TRAUMATOLOGY, OPHTHALMOLOGY")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<ProcedureResponse>> loadTemplate(
            @Parameter(description = "Código de especialidad (ej: DENTISTRY, GENERAL_MEDICINE)")
            @RequestParam String specialty,
            @Parameter(description = "ID de especialidad para asociar (opcional)")
            @RequestParam(required = false) Long specialtyId) {
        List<ProcedureResponse> procedures = procedureService.loadTemplate(specialty, specialtyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(procedures);
    }

    @GetMapping("/templates/available")
    @Operation(summary = "Listar plantillas disponibles",
               description = "Lista todas las especialidades disponibles para carga de plantillas")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<Map<String, List<String>>> getAvailableTemplates() {
        List<String> templates = procedureService.getAvailableTemplates();
        return ResponseEntity.ok(Map.of("availableTemplates", templates));
    }

    @GetMapping("/debug")
    @Operation(summary = "Debug de procedimientos", description = "Endpoint temporal para diagnosticar problemas con procedimientos")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<Map<String, Object>> debugProcedures() {
        return ResponseEntity.ok(procedureService.debugProcedures());
    }
}

