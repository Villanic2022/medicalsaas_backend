package com.saas.medical.controller;

import com.saas.medical.model.dto.insurance.InsuranceCompanyResponse;
import com.saas.medical.service.InsuranceCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/insurance-companies")
@RequiredArgsConstructor
@Tag(name = "Obras Sociales", description = "Gestión de obras sociales y prepagas")
@SecurityRequirement(name = "bearerAuth")
public class InsuranceCompanyController {

    private final InsuranceCompanyService insuranceCompanyService;

    @GetMapping
    @Operation(summary = "Listar obras sociales", description = "Obtiene todas las obras sociales activas")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'STAFF', 'PROFESSIONAL')")
    public ResponseEntity<List<InsuranceCompanyResponse>> findAll() {
        List<InsuranceCompanyResponse> companies = insuranceCompanyService.findAllActive();
        return ResponseEntity.ok(companies);
    }
}
