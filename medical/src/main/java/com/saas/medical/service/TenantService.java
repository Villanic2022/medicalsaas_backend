package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
// import com.saas.medical.model.dto.tenant.CreateTenantRequest;
import com.saas.medical.model.dto.tenant.TenantResponse;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    public Page<TenantResponse> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public TenantResponse getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + id));
        return mapToResponse(tenant);
    }

    public TenantResponse getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + slug));
        return mapToResponse(tenant);
    }

    /*
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        // Método comentado temporalmente por problemas de compilación
        throw new UnsupportedOperationException("Método no implementado");
    }

    @Transactional
    public TenantResponse updateTenant(UUID id, CreateTenantRequest request) {
        // Método comentado temporalmente por problemas de compilación
        throw new UnsupportedOperationException("Método no implementado");
    }
    */

    @Transactional
    public void activateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + id));
        
        tenant.setActive(true);
        tenantRepository.save(tenant);
        log.info("Tenant activado: {}", tenant.getName());
    }

    @Transactional
    public void deactivateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + id));
        
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Tenant desactivado: {}", tenant.getName());
    }

    @Transactional(readOnly = true)
    public TenantResponse findBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlugAndActive(slug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + slug));

        return mapToResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant findEntityBySlug(String slug) {
        return tenantRepository.findBySlugAndActive(slug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + slug));
    }

    @Transactional(readOnly = true)
    public Tenant findEntityById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + id));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .email(tenant.getEmail())
                .phone(tenant.getPhone())
                .address(tenant.getAddress())
                .city(tenant.getCity())
                .timezone(tenant.getTimezone())
                .appointmentDurationMinutes(tenant.getAppointmentDurationMinutes())
                .active(tenant.getActive())
                .build();
    }
}
