package com.saas.medical.controller;

// import com.saas.medical.model.dto.tenant.CreateTenantRequest;
import com.saas.medical.model.dto.tenant.TenantResponse;
import com.saas.medical.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Administración de Tenants", description = "Gestión de inquilinos/clientes del SaaS")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/test")
    @Operation(summary = "Test de autenticación", description = "Verifica si el usuario puede acceder a endpoints de admin")
    public ResponseEntity<String> testAuth(Authentication authentication) {
        return ResponseEntity.ok("Acceso autorizado para: " + authentication.getName() + 
                               " con autoridades: " + authentication.getAuthorities());
    }

    @GetMapping
    @Operation(summary = "Listar tenants", description = "Obtiene lista paginada de todos los tenants")
    public ResponseEntity<Page<TenantResponse>> getAllTenants(Pageable pageable) {
        Page<TenantResponse> tenants = tenantService.getAllTenants(pageable);
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tenant", description = "Obtiene un tenant específico por ID")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        TenantResponse tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Obtener tenant por slug", description = "Obtiene un tenant específico por slug")
    public ResponseEntity<TenantResponse> getTenantBySlug(@PathVariable String slug) {
        TenantResponse tenant = tenantService.getTenantBySlug(slug);
        return ResponseEntity.ok(tenant);
    }

    /*
    @PostMapping
    @Operation(summary = "Crear tenant", description = "Crea un nuevo tenant")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantResponse tenant = tenantService.createTenant(request);
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tenant", description = "Actualiza un tenant existente")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTenantRequest request) {
        TenantResponse tenant = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(tenant);
    }
    */

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activar tenant", description = "Activa un tenant")
    public ResponseEntity<Void> activateTenant(@PathVariable UUID id) {
        tenantService.activateTenant(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar tenant", description = "Desactiva un tenant")
    public ResponseEntity<Void> deactivateTenant(@PathVariable UUID id) {
        tenantService.deactivateTenant(id);
        return ResponseEntity.ok().build();
    }
}