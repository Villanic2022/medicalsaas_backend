package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.professional.ProfessionalRequest;
import com.saas.medical.model.dto.professional.ProfessionalResponse;
import com.saas.medical.model.dto.professional.ProfessionalAvailabilityRequest;
import com.saas.medical.model.dto.professional.ProfessionalAvailabilityResponse;
import com.saas.medical.model.entity.Professional;
import com.saas.medical.model.entity.ProfessionalAvailability;
import com.saas.medical.model.entity.Specialty;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.model.entity.InsuranceCompany;
import com.saas.medical.model.entity.User;
import com.saas.medical.model.entity.Role;
import com.saas.medical.model.enums.DayOfWeek;
import com.saas.medical.repository.ProfessionalRepository;
import com.saas.medical.repository.ProfessionalAvailabilityRepository;
import com.saas.medical.repository.SpecialtyRepository;
import com.saas.medical.repository.TenantRepository;
import com.saas.medical.repository.UserRepository;
import com.saas.medical.repository.RoleRepository;
import com.saas.medical.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalAvailabilityRepository professionalAvailabilityRepository;
    private final SpecialtyRepository specialtyRepository;
    private final TenantRepository tenantRepository;
    private final InsuranceCompanyService insuranceCompanyService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> findAllByTenant() {
        try {
            UUID tenantId = getCurrentTenantId();
            log.info("Buscando profesionales para tenant: {}", tenantId);
            
            List<Professional> professionals = professionalRepository.findByTenantIdWithSpecialty(tenantId);
            log.info("Encontrados {} profesionales", professionals.size());
            
            if (professionals.isEmpty()) {
                log.warn("No se encontraron profesionales para tenant: {}", tenantId);
                return new ArrayList<>();
            }
            
            List<ProfessionalResponse> responses = new ArrayList<>();
            for (Professional professional : professionals) {
                try {
                    log.debug("Mapeando profesional: {} {}", professional.getFirstName(), professional.getLastName());
                    ProfessionalResponse response = mapToResponse(professional);
                    responses.add(response);
                    log.debug("Profesional mapeado exitosamente");
                } catch (Exception e) {
                    log.error("Error mapeando profesional ID {}: {}", professional.getId(), e.getMessage(), e);
                    throw e;
                }
            }
            
            log.info("Retornando {} profesionales mapeados", responses.size());
            return responses;
        } catch (Exception e) {
            log.error("Error en findAllByTenant: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> findAllByTenantSlug(String tenantSlug) {
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "slug", tenantSlug));

        List<Professional> professionals = professionalRepository.findByTenantIdWithSpecialty(tenant.getId());
        return professionals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProfessionalAvailabilityResponse> getAvailabilityByTenantSlug(String tenantSlug, Long professionalId) {
        // Validar que el tenant existe y está activo
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "slug", tenantSlug));
        
        // Validar que el profesional existe y pertenece al tenant
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional", "id", professionalId));
        
        if (!professional.getTenantId().equals(tenant.getId())) {
            throw new ResourceNotFoundException("Professional", "id", professionalId);
        }
        
        // Obtener solo las disponibilidades activas
        List<ProfessionalAvailability> availabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndActiveTrue(professionalId);
        
        return availabilities.stream()
                .map(this::mapToAvailabilityResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionalResponse findById(Long id) {
        try {
            UUID tenantId = getCurrentTenantId();
            log.debug("Buscando profesional ID: {} para tenant: {}", id, tenantId);
            
            Professional professional = professionalRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> {
                        log.warn("Profesional no encontrado - ID: {}, Tenant: {}", id, tenantId);
                        return new ResourceNotFoundException("Profesional no encontrado con ID: " + id + " en el consultorio actual");
                    });
                    
            log.debug("Profesional encontrado: {} {}", professional.getFirstName(), professional.getLastName());
            return mapToResponse(professional);
        } catch (Exception e) {
            log.error("Error buscando profesional ID: {}", id, e);
            throw e;
        }
    }

    @Transactional
    public ProfessionalResponse create(ProfessionalRequest request) {
        UUID tenantId = getCurrentTenantId();

        // Verificar si existe un profesional inactivo con este email para reactivarlo
        if (request.getEmail() != null) {
            Optional<Professional> inactiveProfessional = professionalRepository.findInactiveByEmailAndTenantId(request.getEmail(), tenantId);
            if (inactiveProfessional.isPresent()) {
                // Reactivar profesional existente con los nuevos datos
                Professional professional = inactiveProfessional.get();
                return reactivateProfessional(professional, request);
            }
            
            // Validar que no exista email duplicado activo en el tenant
            if (professionalRepository.findActiveByEmailAndTenantId(request.getEmail(), tenantId).isPresent()) {
                throw new BusinessException("Ya existe un profesional activo con este email en el consultorio");
            }
        }

        // Verificar si existe un profesional inactivo con esta licencia para reactivarlo
        if (request.getLicenseNumber() != null) {
            Optional<Professional> inactiveProfessional = professionalRepository.findInactiveByLicenseNumberAndTenantId(request.getLicenseNumber(), tenantId);
            if (inactiveProfessional.isPresent()) {
                // Si ya encontramos uno por email, esto es un conflicto
                if (request.getEmail() != null) {
                    Optional<Professional> emailProfessional = professionalRepository.findInactiveByEmailAndTenantId(request.getEmail(), tenantId);
                    if (emailProfessional.isPresent() && !emailProfessional.get().getId().equals(inactiveProfessional.get().getId())) {
                        throw new BusinessException("El email y número de matrícula pertenecen a profesionales diferentes. Verifique los datos.");
                    }
                }
                // Si no conflictó por email, reactivar
                if (request.getEmail() == null) {
                    return reactivateProfessional(inactiveProfessional.get(), request);
                } else {
                    Optional<Professional> emailProfessional = professionalRepository.findInactiveByEmailAndTenantId(request.getEmail(), tenantId);
                    if (emailProfessional.isPresent() && emailProfessional.get().getId().equals(inactiveProfessional.get().getId())) {
                        return reactivateProfessional(inactiveProfessional.get(), request);
                    }
                }
            }
            
            // Validar que no exista número de matrícula duplicado activo
            if (professionalRepository.existsByLicenseNumberAndTenantId(request.getLicenseNumber(), tenantId)) {
                throw new BusinessException("Ya existe un profesional activo con este número de matrícula en el consultorio");
            }
        }

        // Buscar especialidad
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialty", "id", request.getSpecialtyId()));

        if (!specialty.getActive()) {
            throw new BusinessException("La especialidad seleccionada no está activa");
        }

        // Crear usuario si se proporciona password
        User user = null;
        if (request.getPassword() != null && !request.getPassword().isBlank() && request.getEmail() != null) {
            // Validar que no exista un usuario con ese email
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Ya existe un usuario con este email");
            }

            // Obtener rol PROFESSIONAL
            Role professionalRole = roleRepository.findByName("PROFESSIONAL")
                    .orElseThrow(() -> new BusinessException("Rol PROFESSIONAL no encontrado en el sistema"));

            // Crear el usuario
            user = new User();
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setTenantId(tenantId);
            user.setRoles(Set.of(professionalRole));
            user.setActive(true);
            user.setEmailVerified(true);
            user = userRepository.save(user);
            log.info("Usuario PROFESSIONAL creado: {} para tenant: {}", user.getEmail(), tenantId);
        }

        // Crear professional
        Professional professional = new Professional();
        professional.setTenantId(tenantId);
        professional.setSpecialty(specialty);
        professional.setFirstName(request.getFirstName());
        professional.setLastName(request.getLastName());
        professional.setLicenseNumber(request.getLicenseNumber());
        professional.setEmail(request.getEmail());
        professional.setPhone(request.getPhone());
        professional.setBio(request.getBio());
        professional.setPrivateConsultationPrice(request.getPrivateConsultationPrice());
        professional.setActive(request.getActive());

        // Vincular con usuario si se creó
        if (user != null) {
            professional.setUser(user);
        }

        // Manejar obras sociales
        if (request.getAcceptedInsurances() != null && !request.getAcceptedInsurances().isEmpty()) {
            List<InsuranceCompany> insurances = insuranceCompanyService.findByIds(request.getAcceptedInsurances());
            professional.getAcceptedInsurances().clear();
            professional.getAcceptedInsurances().addAll(insurances);
        }

        professional = professionalRepository.save(professional);
        log.info("Professional creado: {} {} para tenant: {}",
                professional.getFirstName(), professional.getLastName(), tenantId);

        return mapToResponse(professional);
    }

    private ProfessionalResponse reactivateProfessional(Professional professional, ProfessionalRequest request) {
        // Buscar especialidad
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialty", "id", request.getSpecialtyId()));

        if (!specialty.getActive()) {
            throw new BusinessException("La especialidad seleccionada no está activa");
        }

        // Crear o actualizar usuario si se proporciona password
        if (request.getPassword() != null && !request.getPassword().isBlank() && request.getEmail() != null) {
            User user = professional.getUser();
            if (user == null) {
                // Validar que no exista un usuario con ese email
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new BusinessException("Ya existe un usuario con este email");
                }

                // Obtener rol PROFESSIONAL
                Role professionalRole = roleRepository.findByName("PROFESSIONAL")
                        .orElseThrow(() -> new BusinessException("Rol PROFESSIONAL no encontrado en el sistema"));

                // Crear el usuario
                user = new User();
                user.setEmail(request.getEmail());
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setTenantId(professional.getTenantId());
                user.setRoles(Set.of(professionalRole));
                user.setActive(true);
                user.setEmailVerified(true);
                user = userRepository.save(user);
                professional.setUser(user);
                log.info("Usuario PROFESSIONAL creado para profesional reactivado: {}", user.getEmail());
            } else {
                // Actualizar usuario existente
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                user.setFirstName(request.getFirstName());
                user.setLastName(request.getLastName());
                user.setActive(true);
                userRepository.save(user);
                log.info("Usuario PROFESSIONAL actualizado: {}", user.getEmail());
            }
        }

        // Actualizar datos del profesional
        professional.setSpecialty(specialty);
        professional.setFirstName(request.getFirstName());
        professional.setLastName(request.getLastName());
        professional.setLicenseNumber(request.getLicenseNumber());
        professional.setEmail(request.getEmail());
        professional.setPhone(request.getPhone());
        professional.setBio(request.getBio());
        professional.setPrivateConsultationPrice(request.getPrivateConsultationPrice());
        professional.setActive(true); // Reactivar

        // Manejar obras sociales
        if (request.getAcceptedInsurances() != null && !request.getAcceptedInsurances().isEmpty()) {
            List<InsuranceCompany> insurances = insuranceCompanyService.findByIds(request.getAcceptedInsurances());
            professional.getAcceptedInsurances().clear();
            professional.getAcceptedInsurances().addAll(insurances);
        } else {
            professional.getAcceptedInsurances().clear();
        }

        professional = professionalRepository.save(professional);
        log.info("Professional reactivado: {} {} (ID: {}) para tenant: {}",
                professional.getFirstName(), professional.getLastName(), 
                professional.getId(), professional.getTenantId());

        return mapToResponse(professional);
    }

    @Transactional
    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        UUID tenantId = getCurrentTenantId();

        Professional professional = professionalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional", "id", id));

        // Validar email único (excluyendo el actual y solo entre activos)
        if (request.getEmail() != null && !request.getEmail().equals(professional.getEmail())) {
            Optional<Professional> existingActive = professionalRepository.findActiveByEmailAndTenantId(request.getEmail(), tenantId);
            if (existingActive.isPresent()) {
                throw new BusinessException("Ya existe un profesional activo con este email en el consultorio");
            }
        }

        // Validar número de matrícula único (excluyendo el actual y solo entre activos)
        if (request.getLicenseNumber() != null &&
            !request.getLicenseNumber().equals(professional.getLicenseNumber()) &&
            professionalRepository.existsByLicenseNumberAndTenantId(request.getLicenseNumber(), tenantId)) {
            // Verificar si el conflicto es solo con profesionales inactivos
            Optional<Professional> existingInactive = professionalRepository.findInactiveByLicenseNumberAndTenantId(request.getLicenseNumber(), tenantId);
            if (existingInactive.isPresent()) {
                throw new BusinessException("El número de matrícula pertenece a un profesional inactivo. Contact al administrador si necesita reactivarlo.");
            } else {
                throw new BusinessException("Ya existe un profesional activo con este número de matrícula en el consultorio");
            }
        }

        // Actualizar especialidad si cambió
        if (!request.getSpecialtyId().equals(professional.getSpecialty().getId())) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Specialty", "id", request.getSpecialtyId()));

            if (!specialty.getActive()) {
                throw new BusinessException("La especialidad seleccionada no está activa");
            }
            professional.setSpecialty(specialty);
        }

        // Actualizar campos
        professional.setFirstName(request.getFirstName());
        professional.setLastName(request.getLastName());
        professional.setLicenseNumber(request.getLicenseNumber());
        professional.setEmail(request.getEmail());
        professional.setPhone(request.getPhone());
        professional.setBio(request.getBio());
        professional.setPrivateConsultationPrice(request.getPrivateConsultationPrice());
        professional.setActive(request.getActive());

        // Manejar obras sociales
        if (request.getAcceptedInsurances() != null) {
            if (request.getAcceptedInsurances().isEmpty()) {
                professional.getAcceptedInsurances().clear();
            } else {
                List<InsuranceCompany> insurances = insuranceCompanyService.findByIds(request.getAcceptedInsurances());
                professional.getAcceptedInsurances().clear();
                professional.getAcceptedInsurances().addAll(insurances);
            }
        }

        professional = professionalRepository.save(professional);
        log.info("Professional actualizado: {} {}", professional.getFirstName(), professional.getLastName());

        return mapToResponse(professional);
    }

    @Transactional
    public void delete(Long id) {
        UUID tenantId = getCurrentTenantId();

        Professional professional = professionalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional", "id", id));

        // Soft delete - marcar como inactivo
        professional.setActive(false);
        professionalRepository.save(professional);

        log.info("Professional desactivado: {} {}", professional.getFirstName(), professional.getLastName());
    }

    private ProfessionalResponse mapToResponse(Professional professional) {
        List<ProfessionalResponse.InsuranceCompanyInfo> insuranceInfos = professional.getAcceptedInsurances().stream()
                .map(insurance -> ProfessionalResponse.InsuranceCompanyInfo.builder()
                        .id(insurance.getId())
                        .name(insurance.getName())
                        .code(insurance.getCode())
                        .build())
                .collect(Collectors.toList());

        return ProfessionalResponse.builder()
                .id(professional.getId())
                .firstName(professional.getFirstName())
                .lastName(professional.getLastName())
                .fullName(professional.getFullName())
                .licenseNumber(professional.getLicenseNumber())
                .email(professional.getEmail())
                .phone(professional.getPhone())
                .bio(professional.getBio())
                .active(professional.getActive())
                .privateConsultationPrice(professional.getPrivateConsultationPrice())
                .acceptedInsurances(insuranceInfos)
                .specialty(ProfessionalResponse.SpecialtyInfo.builder()
                        .id(professional.getSpecialty().getId())
                        .name(professional.getSpecialty().getName())
                        .description(professional.getSpecialty().getDescription())
                        .build())
                .build();
    }

    // ==================== MÉTODOS DE DISPONIBILIDAD ====================

    @Transactional(readOnly = true)
    public List<ProfessionalAvailabilityResponse> getAvailability(Long professionalId) {
        // Verificar que el professional existe y pertenece al tenant actual
        Professional professional = validateProfessionalAccess(professionalId);
        
        List<ProfessionalAvailability> availabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndActiveTrue(professionalId);
        
        return availabilities.stream()
                .map(this::mapToAvailabilityResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfessionalAvailabilityResponse addAvailability(Long professionalId, ProfessionalAvailabilityRequest request) {
        // DEBUG: Logging para ver qué llega en el request
        log.info("🔍 DEBUG addAvailability - specificDate: {}, dayOfWeek: {}, startTime: {}, endTime: {}", 
                request.getSpecificDate(), request.getDayOfWeek(), request.getStartTime(), request.getEndTime());
        
        // Validaciones
        validateAvailabilityRequest(request);
        Professional professional = validateProfessionalAccess(professionalId);
        
        // Verificar que no exista solapamiento de horarios
        if (request.getSpecificDate() != null) {
            // Para fecha específica, verificar solapamientos en esa fecha
            validateNoTimeOverlapForDate(professionalId, request.getSpecificDate(), 
                    request.getStartTime(), request.getEndTime(), null);
        } else {
            // Para configuración recurrente, verificar solapamientos en el día de la semana
            if (request.getDayOfWeek() == null) {
                throw new BusinessException("Debe especificar un día de la semana para configuraciones recurrentes");
            }
            validateNoTimeOverlap(professionalId, request.getDayOfWeek(), 
                    request.getStartTime(), request.getEndTime(), null);
        }
        
        ProfessionalAvailability availability = new ProfessionalAvailability();
        availability.setProfessional(professional);
        
        // Lógica: Si hay fecha específica, usar solo esa. Si no, usar dayOfWeek
        if (request.getSpecificDate() != null) {
            log.info("📅 Creando disponibilidad ESPECÍFICA para fecha: {}", request.getSpecificDate());
            availability.setSpecificDate(request.getSpecificDate());
            availability.setDayOfWeek(null); // Ignorar dayOfWeek para fechas específicas
        } else {
            log.info("📅 Creando disponibilidad RECURRENTE para día: {}", request.getDayOfWeek());
            availability.setDayOfWeek(request.getDayOfWeek());
            availability.setSpecificDate(null);
        }
        
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
        availability.setActive(request.getActive());
        
        availability = professionalAvailabilityRepository.save(availability);
        
        if (request.getSpecificDate() != null) {
            log.info("Disponibilidad específica creada para fecha {}: {} para professional {}", 
                    request.getSpecificDate(), 
                    request.getStartTime() + "-" + request.getEndTime(), 
                    professional.getFullName());
        } else {
            log.info("Disponibilidad recurrente creada: {} para professional {}", 
                    request.getDayOfWeek().getDisplayName(), professional.getFullName());
        }
        
        return mapToAvailabilityResponse(availability);
    }

    @Transactional
    public List<ProfessionalAvailabilityResponse> updateAvailability(Long professionalId, 
                                                                     List<ProfessionalAvailabilityRequest> requests) {
        log.info("🔍 DEBUG updateAvailability - Iniciando para professionalId: {}, requests: {}", professionalId, requests.size());
        
        // Log detallado de cada request
        for (int i = 0; i < requests.size(); i++) {
            ProfessionalAvailabilityRequest req = requests.get(i);
            log.info("🔍 Request[{}]: specificDate={}, dayOfWeek={}, startTime={}, endTime={}, slotDuration={}", 
                    i, req.getSpecificDate(), req.getDayOfWeek(), req.getStartTime(), req.getEndTime(), req.getSlotDurationMinutes());
        }
        
        // Verificar que el professional existe y pertenece al tenant actual
        Professional professional = validateProfessionalAccess(professionalId);
        
        // Validar todas las requests
        requests.forEach(this::validateAvailabilityRequest);
        
        // Eliminar disponibilidades existentes
        professionalAvailabilityRepository.deleteByProfessionalId(professionalId);
        
        // Crear nuevas disponibilidades
        List<ProfessionalAvailabilityResponse> responses = new ArrayList<>();
        for (ProfessionalAvailabilityRequest request : requests) {
            log.info("🔍 DEBUG updateAvailability - specificDate: {}, dayOfWeek: {}", 
                    request.getSpecificDate(), request.getDayOfWeek());
            
            // Validar solapamientos entre las nuevas configuraciones
            validateNoTimeOverlapInList(requests, request);
            
            ProfessionalAvailability availability = new ProfessionalAvailability();
            availability.setProfessional(professional);
            
            // Lógica: Si hay fecha específica, usar solo esa. Si no, usar dayOfWeek
            if (request.getSpecificDate() != null) {
                log.info("📅 Actualizando con disponibilidad ESPECÍFICA para fecha: {}", request.getSpecificDate());
                availability.setSpecificDate(request.getSpecificDate());
                availability.setDayOfWeek(null); // Ignorar dayOfWeek para fechas específicas
            } else {
                log.info("📅 Actualizando con disponibilidad RECURRENTE para día: {}", request.getDayOfWeek());
                availability.setDayOfWeek(request.getDayOfWeek());
                availability.setSpecificDate(null);
            }
            
            availability.setStartTime(request.getStartTime());
            availability.setEndTime(request.getEndTime());
            availability.setSlotDurationMinutes(request.getSlotDurationMinutes());
            availability.setActive(request.getActive());
            
            availability = professionalAvailabilityRepository.save(availability);
            responses.add(mapToAvailabilityResponse(availability));
        }
        
        log.info("Disponibilidades actualizadas para professional {}: {} configuraciones", 
                professional.getFullName(), requests.size());
        
        return responses;
    }

    /**
     * Busca disponibilidad para una fecha específica, priorizando configuraciones específicas sobre recurrentes
     */
    @Transactional(readOnly = true)
    public List<ProfessionalAvailabilityResponse> getAvailabilityForDate(Long professionalId, LocalDate date) {
        Professional professional = validateProfessionalAccess(professionalId);
        
        // Primero buscar configuraciones específicas para esa fecha
        List<ProfessionalAvailability> specificAvailabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndSpecificDateAndActive(professionalId, date);
        
        if (!specificAvailabilities.isEmpty()) {
            // Si existe configuración específica, usarla (incluso si está vacía para indicar día libre)
            return specificAvailabilities.stream()
                    .map(this::mapToAvailabilityResponse)
                    .collect(Collectors.toList());
        }
        
        // Si no hay configuración específica, usar la configuración recurrente del día de la semana
        DayOfWeek dayOfWeek = DayOfWeek.fromLocalDate(date);
        List<ProfessionalAvailability> recurringAvailabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndDayOfWeekAndActive(professionalId, dayOfWeek);
        
        return recurringAvailabilities.stream()
                .map(this::mapToAvailabilityResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAvailability(Long availabilityId) {
        UUID tenantId = getCurrentTenantId();
        
        ProfessionalAvailability availability = professionalAvailabilityRepository
                .findByIdAndTenantId(availabilityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada: " + availabilityId));
        
        professionalAvailabilityRepository.delete(availability);
        log.info("Disponibilidad eliminada: {} para professional {}", 
                availability.getDayOfWeek().getDisplayName(), 
                availability.getProfessional().getFullName());
    }

    // ==================== MÉTODOS PRIVADOS DE DISPONIBILIDAD ====================

    private Professional validateProfessionalAccess(Long professionalId) {
        UUID tenantId = getCurrentTenantId();
        
        return professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Professional no encontrado: " + professionalId));
    }

    /**
     * Valida que el usuario autenticado con rol PROFESSIONAL solo pueda acceder a su propia disponibilidad.
     * Los roles ADMIN y OWNER pueden acceder a cualquier profesional.
     */
    public void validateProfessionalSelfAccess(Long professionalId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si el usuario tiene rol ADMIN u OWNER, permitir acceso sin restricción
        boolean isAdminOrOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_OWNER"));

        if (isAdminOrOwner) {
            log.debug("Usuario con rol ADMIN/OWNER, acceso permitido al profesional {}", professionalId);
            return;
        }

        // Para rol PROFESSIONAL, verificar que solo acceda a su propia disponibilidad
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new BusinessException("Usuario no encontrado", HttpStatus.UNAUTHORIZED);
        }

        User user = userOpt.get();
        Optional<Professional> professionalOpt = professionalRepository.findByUserId(user.getId());

        if (professionalOpt.isEmpty()) {
            throw new BusinessException("El usuario no tiene un perfil profesional vinculado", HttpStatus.FORBIDDEN);
        }

        Professional authenticatedProfessional = professionalOpt.get();

        if (!authenticatedProfessional.getId().equals(professionalId)) {
            log.warn("Profesional {} intentó acceder a disponibilidad del profesional {}",
                    authenticatedProfessional.getId(), professionalId);
            throw new BusinessException("No tiene permiso para gestionar la disponibilidad de otro profesional", HttpStatus.FORBIDDEN);
        }

        log.debug("Profesional {} accediendo a su propia disponibilidad", professionalId);
    }

    /**
     * Valida que el usuario autenticado con rol PROFESSIONAL solo pueda eliminar sus propias reglas de disponibilidad.
     * Los roles ADMIN y OWNER pueden eliminar cualquier disponibilidad.
     */
    public void validateAvailabilityOwnership(Long availabilityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si el usuario tiene rol ADMIN u OWNER, permitir acceso sin restricción
        boolean isAdminOrOwner = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_OWNER"));

        if (isAdminOrOwner) {
            log.debug("Usuario con rol ADMIN/OWNER, acceso permitido para eliminar disponibilidad {}", availabilityId);
            return;
        }

        // Para rol PROFESSIONAL, verificar que la disponibilidad le pertenezca
        UUID tenantId = getCurrentTenantId();
        ProfessionalAvailability availability = professionalAvailabilityRepository
                .findByIdAndTenantId(availabilityId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada: " + availabilityId));

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new BusinessException("Usuario no encontrado", HttpStatus.UNAUTHORIZED);
        }

        User user = userOpt.get();
        Optional<Professional> professionalOpt = professionalRepository.findByUserId(user.getId());

        if (professionalOpt.isEmpty()) {
            throw new BusinessException("El usuario no tiene un perfil profesional vinculado", HttpStatus.FORBIDDEN);
        }

        Professional authenticatedProfessional = professionalOpt.get();

        if (!availability.getProfessional().getId().equals(authenticatedProfessional.getId())) {
            log.warn("Profesional {} intentó eliminar disponibilidad {} que pertenece al profesional {}",
                    authenticatedProfessional.getId(), availabilityId, availability.getProfessional().getId());
            throw new BusinessException("No tiene permiso para eliminar la disponibilidad de otro profesional", HttpStatus.FORBIDDEN);
        }

        log.debug("Profesional {} eliminando su propia disponibilidad {}", authenticatedProfessional.getId(), availabilityId);
    }

    /**
     * Valida acceso para lectura de disponibilidad.
     * ADMIN, OWNER y STAFF pueden ver cualquier disponibilidad.
     * PROFESSIONAL solo puede ver su propia disponibilidad.
     */
    public void validateProfessionalSelfAccessForRead(Long professionalId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si el usuario tiene rol ADMIN, OWNER o STAFF, permitir acceso sin restricción
        boolean isAdminOwnerOrStaff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN") || auth.equals("ROLE_OWNER") || auth.equals("ROLE_STAFF"));

        if (isAdminOwnerOrStaff) {
            log.debug("Usuario con rol ADMIN/OWNER/STAFF, acceso de lectura permitido al profesional {}", professionalId);
            return;
        }

        // Para rol PROFESSIONAL, verificar que solo acceda a su propia disponibilidad
        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new BusinessException("Usuario no encontrado", HttpStatus.UNAUTHORIZED);
        }

        User user = userOpt.get();
        Optional<Professional> professionalOpt = professionalRepository.findByUserId(user.getId());

        if (professionalOpt.isEmpty()) {
            throw new BusinessException("El usuario no tiene un perfil profesional vinculado", HttpStatus.FORBIDDEN);
        }

        Professional authenticatedProfessional = professionalOpt.get();

        if (!authenticatedProfessional.getId().equals(professionalId)) {
            log.warn("Profesional {} intentó ver disponibilidad del profesional {}",
                    authenticatedProfessional.getId(), professionalId);
            throw new BusinessException("No tiene permiso para ver la disponibilidad de otro profesional", HttpStatus.FORBIDDEN);
        }

        log.debug("Profesional {} consultando su propia disponibilidad", professionalId);
    }

    private void validateAvailabilityRequest(ProfessionalAvailabilityRequest request) {
        // Validación null-safe para evitar NPE
        if (request.getStartTime() == null) {
            throw new BusinessException("La hora de inicio es requerida");
        }
        
        if (request.getEndTime() == null) {
            throw new BusinessException("La hora de fin es requerida");
        }
        
        if (request.getSlotDurationMinutes() == null) {
            throw new BusinessException("La duración del turno es requerida");
        }
        
        if (request.getStartTime().isAfter(request.getEndTime()) || 
            request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessException("La hora de inicio debe ser menor que la hora de fin");
        }
        
        if (request.getSlotDurationMinutes() < 5 || request.getSlotDurationMinutes() > 120) {
            throw new BusinessException("La duración del turno debe estar entre 5 y 120 minutos");
        }
        
        // Validar que se especifique al menos una fecha específica o un día recurrente
        if (request.getSpecificDate() == null && request.getDayOfWeek() == null) {
            throw new BusinessException("Debe especificar una fecha específica o un día de la semana recurrente");
        }
        
        // Si se especifica fecha específica, se ignora dayOfWeek automáticamente
        // No es necesario validar que ambos estén presentes
    }

    private void validateNoTimeOverlap(Long professionalId, com.saas.medical.model.enums.DayOfWeek dayOfWeek, 
                                     LocalTime newStartTime, LocalTime newEndTime, Long excludeAvailabilityId) {
        List<ProfessionalAvailability> existingAvailabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndDayOfWeekAndActive(professionalId, dayOfWeek);
        
        for (ProfessionalAvailability existing : existingAvailabilities) {
            if (excludeAvailabilityId != null && existing.getId().equals(excludeAvailabilityId)) {
                continue;
            }
            
            if (timesOverlap(newStartTime, newEndTime, existing.getStartTime(), existing.getEndTime())) {
                throw new BusinessException(String.format(
                    "El horario solapa con una configuración existente para %s (%s - %s)",
                    dayOfWeek.getDisplayName(),
                    existing.getStartTime(),
                    existing.getEndTime()
                ));
            }
        }
    }

    private void validateNoTimeOverlapForDate(Long professionalId, LocalDate specificDate, 
                                            LocalTime newStartTime, LocalTime newEndTime, Long excludeAvailabilityId) {
        List<ProfessionalAvailability> existingAvailabilities = professionalAvailabilityRepository
                .findByProfessionalIdAndSpecificDateAndActive(professionalId, specificDate);
        
        for (ProfessionalAvailability existing : existingAvailabilities) {
            if (excludeAvailabilityId != null && existing.getId().equals(excludeAvailabilityId)) {
                continue;
            }
            
            if (timesOverlap(newStartTime, newEndTime, existing.getStartTime(), existing.getEndTime())) {
                throw new BusinessException(String.format(
                    "El horario solapa con una configuración existente para la fecha %s (%s - %s)",
                    specificDate,
                    existing.getStartTime(),
                    existing.getEndTime()
                ));
            }
        }
    }

    private void validateNoTimeOverlapInList(List<ProfessionalAvailabilityRequest> requests, 
                                           ProfessionalAvailabilityRequest current) {
        for (ProfessionalAvailabilityRequest other : requests) {
            if (other == current) continue;
            
            boolean sameTimeSlot = false;
            
            // Comparar si son el mismo slot temporal
            if (current.getSpecificDate() != null && other.getSpecificDate() != null) {
                // Ambas son fechas específicas: comparar fechas exactas
                sameTimeSlot = current.getSpecificDate().equals(other.getSpecificDate());
            } else if (current.getDayOfWeek() != null && other.getDayOfWeek() != null) {
                // Ambas son días recurrentes: comparar días de la semana
                sameTimeSlot = current.getDayOfWeek().equals(other.getDayOfWeek());
            }
            // Si una es fecha específica y otra día recurrente, NO hay solapamiento
            
            if (sameTimeSlot &&
                timesOverlap(current.getStartTime(), current.getEndTime(), 
                           other.getStartTime(), other.getEndTime())) {
                
                String timeSlotDescription = current.getSpecificDate() != null 
                    ? "fecha " + current.getSpecificDate()
                    : "día " + (current.getDayOfWeek() != null ? current.getDayOfWeek().getDisplayName() : "desconocido");
                    
                throw new BusinessException(String.format(
                    "Los horarios solapan para %s: (%s - %s) con (%s - %s)",
                    timeSlotDescription,
                    current.getStartTime(), current.getEndTime(),
                    other.getStartTime(), other.getEndTime()
                ));
            }
        }
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private ProfessionalAvailabilityResponse mapToAvailabilityResponse(ProfessionalAvailability availability) {
        return ProfessionalAvailabilityResponse.builder()
                .id(availability.getId())
                .dayOfWeek(availability.getDayOfWeek())
                .dayOfWeekDisplay(availability.getDayOfWeek() != null ? availability.getDayOfWeek().getDisplayName() : null)
                .specificDate(availability.getSpecificDate())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .slotDurationMinutes(availability.getSlotDurationMinutes())
                .active(availability.getActive())
                .createdAt(availability.getCreatedAt())
                .professionalId(availability.getProfessional().getId())
                .professionalName(availability.getProfessional().getFullName())
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

    @Transactional(readOnly = true)
    public long countAll() {
        return professionalRepository.count();
    }
}
