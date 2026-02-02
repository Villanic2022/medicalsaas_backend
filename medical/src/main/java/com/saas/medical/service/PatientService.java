package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.insurance.InsuranceCompanyResponse;
import com.saas.medical.model.dto.patient.PatientRequest;
import com.saas.medical.model.dto.patient.PatientResponse;
import com.saas.medical.model.dto.patient.PatientSearchResponse;
import com.saas.medical.model.dto.patient.PatientUpdateRequest;
import com.saas.medical.model.dto.professional.ProfessionalResponse;
import com.saas.medical.model.entity.InsuranceCompany;
import com.saas.medical.model.entity.Patient;
import com.saas.medical.model.entity.Professional;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.repository.InsuranceCompanyRepository;
import com.saas.medical.repository.PatientRepository;
import com.saas.medical.repository.ProfessionalRepository;
import com.saas.medical.repository.TenantRepository;
import com.saas.medical.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final TenantRepository tenantRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional(readOnly = true)
    public List<PatientResponse> findAllByTenant(String search, Long insuranceId, Long professionalId) {
        try {
            UUID tenantId = getCurrentTenantId();
            log.info("Buscando pacientes para tenant: {} con filtros - search: {}, insuranceId: {}, professionalId: {}", 
                     tenantId, search, insuranceId, professionalId);
            
            List<Patient> patients = patientRepository.findPatientsWithFilters(tenantId, search, insuranceId, professionalId);
            log.info("Encontrados {} pacientes", patients.size());
            
            // Cargar manualmente las relaciones que no se cargan con query nativa
            patients.forEach(patient -> {
                if (patient.getInsuranceCompany() != null) {
                    patient.getInsuranceCompany().getName(); // trigger lazy loading
                }
                if (patient.getPreferredProfessional() != null) {
                    patient.getPreferredProfessional().getFirstName(); // trigger lazy loading
                }
            });
            
            return patients.stream()
                    .map(this::mapToPatientResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error al buscar pacientes", e);
            throw new BusinessException("Error al buscar pacientes: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(Long id) {
        UUID tenantId = getCurrentTenantId();
        log.info("Buscando paciente {} para tenant: {}", id, tenantId);
        
        Patient patient = patientRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + id));
        
        return mapToPatientResponse(patient);
    }

    @Transactional(readOnly = true)
    public Optional<PatientResponse> findByDni(String dni) {
        UUID tenantId = getCurrentTenantId();
        log.info("Buscando paciente por DNI: {} para tenant: {}", dni, tenantId);
        
        return patientRepository.findByTenantIdAndDni(tenantId, dni)
                .map(this::mapToPatientResponse);
    }

    @Transactional(readOnly = true)
    public List<PatientSearchResponse> searchPatients(String query) {
        UUID tenantId = getCurrentTenantId();
        log.info("Búsqueda rápida de pacientes para tenant: {} con query: {}", tenantId, query);
        
        Pageable pageable = PageRequest.of(0, 10);
        List<Patient> patients = patientRepository.searchPatients(tenantId, query, pageable);
        
        // Cargar relaciones manualmente para queries nativas
        patients.forEach(patient -> {
            if (patient.getInsuranceCompany() != null) {
                patient.getInsuranceCompany().getName(); // trigger lazy loading
            }
            if (patient.getPreferredProfessional() != null) {
                patient.getPreferredProfessional().getFirstName(); // trigger lazy loading
            }
        });
        
        return patients.stream()
                .map(this::mapToPatientSearchResponse)
                .toList();
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        UUID tenantId = getCurrentTenantId();
        log.info("Creando paciente para tenant: {} con DNI: {}", tenantId, request.getDni());
        
        // Validar que el DNI no exista
        if (patientRepository.existsByTenantIdAndDni(tenantId, request.getDni())) {
            throw new BusinessException("Ya existe un paciente con el DNI: " + request.getDni());
        }
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));
        
        Patient patient = new Patient();
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDni(request.getDni());
        patient.setBirthDate(request.getBirthDate());
        patient.setGender(request.getGender());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setAddress(request.getAddress());
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setNotes(request.getNotes());
        patient.setTenant(tenant);
        patient.setActive(true);
        
        // Asignar obra social si se especifica
        if (request.getInsuranceCompanyId() != null) {
            InsuranceCompany insurance = insuranceCompanyRepository.findById(request.getInsuranceCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada"));
            patient.setInsuranceCompany(insurance);
        }
        
        // Asignar profesional preferido si se especifica
        if (request.getPreferredProfessionalId() != null) {
            Professional professional = professionalRepository.findByTenantIdAndId(tenantId, request.getPreferredProfessionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
            patient.setPreferredProfessional(professional);
        }
        
        Patient savedPatient = patientRepository.save(patient);
        log.info("Paciente creado exitosamente con ID: {}", savedPatient.getId());
        
        return mapToPatientResponse(savedPatient);
    }

    @Transactional
    public PatientResponse update(Long id, PatientUpdateRequest request) {
        UUID tenantId = getCurrentTenantId();
        log.info("Actualizando paciente {} para tenant: {}", id, tenantId);
        
        Patient patient = patientRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + id));
        
        // Validar DNI si ha cambiado
        if (StringUtils.hasText(request.getDni()) && !request.getDni().equals(patient.getDni())) {
            if (patientRepository.existsByTenantIdAndDniAndIdNot(tenantId, request.getDni(), id)) {
                throw new BusinessException("Ya existe otro paciente con el DNI: " + request.getDni());
            }
        }
        
        // Actualizar campos
        if (StringUtils.hasText(request.getFirstName())) {
            patient.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            patient.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getDni())) {
            patient.setDni(request.getDni());
        }
        if (request.getBirthDate() != null) {
            patient.setBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (StringUtils.hasText(request.getEmail())) {
            patient.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            patient.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }
        if (request.getInsuranceNumber() != null) {
            patient.setInsuranceNumber(request.getInsuranceNumber());
        }
        if (request.getNotes() != null) {
            patient.setNotes(request.getNotes());
        }
        
        // Actualizar obra social
        if (request.getInsuranceCompanyId() != null) {
            InsuranceCompany insurance = insuranceCompanyRepository.findById(request.getInsuranceCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Obra social no encontrada"));
            patient.setInsuranceCompany(insurance);
        } else {
            patient.setInsuranceCompany(null);
        }
        
        // Actualizar profesional preferido
        if (request.getPreferredProfessionalId() != null) {
            Professional professional = professionalRepository.findByTenantIdAndId(tenantId, request.getPreferredProfessionalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Profesional no encontrado"));
            patient.setPreferredProfessional(professional);
        } else {
            patient.setPreferredProfessional(null);
        }
        
        Patient updatedPatient = patientRepository.save(patient);
        log.info("Paciente actualizado exitosamente");
        
        return mapToPatientResponse(updatedPatient);
    }

    @Transactional
    public void delete(Long id) {
        UUID tenantId = getCurrentTenantId();
        log.info("Eliminando paciente {} para tenant: {}", id, tenantId);
        
        Patient patient = patientRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + id));
        
        // Soft delete
        patient.setActive(false);
        patientRepository.save(patient);
        
        log.info("Paciente eliminado exitosamente (soft delete)");
    }

    private PatientResponse mapToPatientResponse(Patient patient) {
        PatientResponse response = PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFullName())
                .dni(patient.getDni())
                .birthDate(patient.getBirthDate())
                .gender(patient.getGender())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .address(patient.getAddress())
                .insuranceNumber(patient.getInsuranceNumber())
                .notes(patient.getNotes())
                .createdAt(patient.getCreatedAt())
                .active(patient.getActive())
                .build();
        
        // Mapear obra social si existe
        if (patient.getInsuranceCompany() != null) {
            InsuranceCompanyResponse insuranceResponse = InsuranceCompanyResponse.builder()
                    .id(patient.getInsuranceCompany().getId())
                    .name(patient.getInsuranceCompany().getName())
                    .code(patient.getInsuranceCompany().getCode())
                    .build();
            response.setInsuranceCompany(insuranceResponse);
        }
        
        // Mapear profesional preferido si existe
        if (patient.getPreferredProfessional() != null) {
            ProfessionalResponse professionalResponse = ProfessionalResponse.builder()
                    .id(patient.getPreferredProfessional().getId())
                    .firstName(patient.getPreferredProfessional().getFirstName())
                    .lastName(patient.getPreferredProfessional().getLastName())
                    .fullName(patient.getPreferredProfessional().getFullName())
                    .build();
            response.setPreferredProfessional(professionalResponse);
        }
        
        return response;
    }

    private PatientSearchResponse mapToPatientSearchResponse(Patient patient) {
        return PatientSearchResponse.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .fullName(patient.getFullName())
                .dni(patient.getDni())
                .phone(patient.getPhone())
                .email(patient.getEmail())
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
}