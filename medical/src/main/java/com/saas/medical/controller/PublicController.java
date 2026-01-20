package com.saas.medical.controller;

import com.saas.medical.model.dto.specialty.SpecialtyResponse;
import com.saas.medical.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Endpoints públicos sin autenticación")
public class PublicController {

    private final SpecialtyService specialtyService;

    @GetMapping("/specialties")
    @Operation(summary = "Obtener especialidades", description = "Lista todas las especialidades activas")
    public ResponseEntity<List<SpecialtyResponse>> getSpecialties() {
        List<SpecialtyResponse> specialties = specialtyService.findAllActive();
        return ResponseEntity.ok(specialties);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica que la API esté funcionando")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("API funcionando correctamente");
    }
}
