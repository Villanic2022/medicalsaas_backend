package com.saas.medical.controller;

import com.saas.medical.model.dto.specialty.SpecialtyRequest;
import com.saas.medical.model.dto.specialty.SpecialtyResponse;
import com.saas.medical.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/specialties")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Gestión de especialidades médicas")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    @Operation(summary = "Listar especialidades", description = "Obtiene todas las especialidades activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<List<SpecialtyResponse>> getAllSpecialties() {
        List<SpecialtyResponse> specialties = specialtyService.findAllActive();
        return ResponseEntity.ok(specialties);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener especialidad", description = "Obtiene una especialidad por ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<SpecialtyResponse> getSpecialty(@PathVariable Long id) {
        SpecialtyResponse specialty = specialtyService.findById(id);
        return ResponseEntity.ok(specialty);
    }

    @PostMapping
    @Operation(summary = "Crear especialidad", description = "Crea una nueva especialidad médica")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<SpecialtyResponse> createSpecialty(@Valid @RequestBody SpecialtyRequest request) {
        SpecialtyResponse specialty = specialtyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(specialty);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar especialidad", description = "Desactiva una especialidad médica (soft delete)")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Void> deactivateSpecialty(@PathVariable Long id) {
        specialtyService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}