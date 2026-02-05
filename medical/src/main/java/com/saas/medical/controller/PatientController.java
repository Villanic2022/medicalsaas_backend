package com.saas.medical.controller;

import com.saas.medical.model.dto.patient.PatientRequest;
import com.saas.medical.model.dto.patient.PatientResponse;
import com.saas.medical.model.dto.patient.PatientSearchResponse;
import com.saas.medical.model.dto.patient.PatientUpdateRequest;
import com.saas.medical.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Optional;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pacientes", description = "Gestión de pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Obtiene todos los pacientes del tenant actual con filtros opcionales")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<PatientResponse>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long insuranceId,
            @RequestParam(required = false) Long professionalId) {
        
        log.info("GET /patients - search: {}, insuranceId: {}, professionalId: {}", search, insuranceId, professionalId);
        List<PatientResponse> patients = patientService.findAllByTenant(search, insuranceId, professionalId);
        return ResponseEntity.ok(patients);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paciente por ID", description = "Obtiene un paciente específico por su ID")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PatientResponse> findById(@PathVariable Long id) {
        log.info("GET /patients/{}", id);
        PatientResponse patient = patientService.findById(id);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/by-dni/{dni}")
    @Operation(summary = "Buscar paciente por DNI", description = "Busca un paciente por su número de DNI")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PatientResponse> findByDni(@PathVariable String dni) {
        log.info("GET /patients/by-dni/{}", dni);
        Optional<PatientResponse> patient = patientService.findByDni(dni);
        
        if (patient.isPresent()) {
            return ResponseEntity.ok(patient.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Búsqueda rápida", description = "Búsqueda rápida de pacientes por nombre o DNI (máximo 10 resultados)")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<List<PatientSearchResponse>> searchPatients(@RequestParam String q) {
        log.info("GET /patients/search?q={}", q);
        List<PatientSearchResponse> patients = patientService.searchPatients(q);
        return ResponseEntity.ok(patients);
    }

    @PostMapping
    @Operation(summary = "Crear paciente", description = "Crea un nuevo paciente")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PatientResponse> create(@Valid @RequestBody PatientRequest request) {
        log.info("POST /patients - Creating patient with DNI: {}", request.getDni());
        PatientResponse patient = patientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar paciente", description = "Actualiza los datos de un paciente existente")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<PatientResponse> update(@PathVariable Long id, 
                                                @Valid @RequestBody PatientUpdateRequest request) {
        log.info("PUT /patients/{}", id);
        PatientResponse patient = patientService.update(id, request);
        return ResponseEntity.ok(patient);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar paciente", description = "Elimina un paciente (soft delete)")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /patients/{}", id);
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}