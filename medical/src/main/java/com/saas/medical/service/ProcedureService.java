package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.procedure.ProcedureRequest;
import com.saas.medical.model.dto.procedure.ProcedureResponse;
import com.saas.medical.model.entity.Procedure;
import com.saas.medical.model.entity.Specialty;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.repository.ProcedureRepository;
import com.saas.medical.repository.SpecialtyRepository;
import com.saas.medical.repository.TenantRepository;
import com.saas.medical.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final SpecialtyRepository specialtyRepository;
    private final TenantRepository tenantRepository;

    // Plantillas de procedimientos por especialidad
    private static final Map<String, List<ProcedureTemplate>> PROCEDURE_TEMPLATES = new HashMap<>();

    static {
        // Odontología / Dentistry
        PROCEDURE_TEMPLATES.put("DENTISTRY", Arrays.asList(
                new ProcedureTemplate("Limpieza dental", 30),
                new ProcedureTemplate("Extracción dental simple", 45),
                new ProcedureTemplate("Extracción de muela del juicio", 60),
                new ProcedureTemplate("Empaste/Obturación", 30),
                new ProcedureTemplate("Tratamiento de conducto", 90),
                new ProcedureTemplate("Corona dental", 60),
                new ProcedureTemplate("Blanqueamiento dental", 60),
                new ProcedureTemplate("Ortodoncia - Consulta", 30),
                new ProcedureTemplate("Ortodoncia - Ajuste", 20),
                new ProcedureTemplate("Implante dental - Consulta", 45),
                new ProcedureTemplate("Implante dental - Colocación", 90),
                new ProcedureTemplate("Radiografía dental", 15),
                new ProcedureTemplate("Revisión general", 20)
        ));

        // Medicina General
        PROCEDURE_TEMPLATES.put("GENERAL_MEDICINE", Arrays.asList(
                new ProcedureTemplate("Consulta general", 20),
                new ProcedureTemplate("Control de presión arterial", 15),
                new ProcedureTemplate("Control de diabetes", 30),
                new ProcedureTemplate("Certificado médico", 15),
                new ProcedureTemplate("Vacunación", 15),
                new ProcedureTemplate("Electrocardiograma", 30),
                new ProcedureTemplate("Chequeo anual", 45)
        ));

        // Dermatología
        PROCEDURE_TEMPLATES.put("DERMATOLOGY", Arrays.asList(
                new ProcedureTemplate("Consulta dermatológica", 30),
                new ProcedureTemplate("Biopsia de piel", 45),
                new ProcedureTemplate("Crioterapia", 30),
                new ProcedureTemplate("Tratamiento de acné", 30),
                new ProcedureTemplate("Peeling químico", 45),
                new ProcedureTemplate("Dermatoscopia", 20),
                new ProcedureTemplate("Tratamiento de verrugas", 30)
        ));

        // Cardiología
        PROCEDURE_TEMPLATES.put("CARDIOLOGY", Arrays.asList(
                new ProcedureTemplate("Consulta cardiológica", 30),
                new ProcedureTemplate("Electrocardiograma", 20),
                new ProcedureTemplate("Ecocardiograma", 45),
                new ProcedureTemplate("Holter 24 horas - Colocación", 20),
                new ProcedureTemplate("Holter 24 horas - Retiro", 15),
                new ProcedureTemplate("Prueba de esfuerzo", 60),
                new ProcedureTemplate("Control de marcapasos", 30)
        ));

        // Ginecología
        PROCEDURE_TEMPLATES.put("GYNECOLOGY", Arrays.asList(
                new ProcedureTemplate("Consulta ginecológica", 30),
                new ProcedureTemplate("Papanicolaou", 20),
                new ProcedureTemplate("Ecografía ginecológica", 30),
                new ProcedureTemplate("Ecografía obstétrica", 30),
                new ProcedureTemplate("Colposcopía", 30),
                new ProcedureTemplate("Control prenatal", 30),
                new ProcedureTemplate("Colocación de DIU", 30)
        ));

        // Pediatría
        PROCEDURE_TEMPLATES.put("PEDIATRICS", Arrays.asList(
                new ProcedureTemplate("Consulta pediátrica", 20),
                new ProcedureTemplate("Control de niño sano", 30),
                new ProcedureTemplate("Vacunación", 15),
                new ProcedureTemplate("Control de crecimiento", 30),
                new ProcedureTemplate("Certificado escolar", 15)
        ));

        // Traumatología
        PROCEDURE_TEMPLATES.put("TRAUMATOLOGY", Arrays.asList(
                new ProcedureTemplate("Consulta traumatológica", 30),
                new ProcedureTemplate("Infiltración articular", 30),
                new ProcedureTemplate("Colocación de yeso", 45),
                new ProcedureTemplate("Retiro de yeso", 20),
                new ProcedureTemplate("Control post-operatorio", 20),
                new ProcedureTemplate("Kinesiología - Sesión", 45)
        ));

        // Oftalmología
        PROCEDURE_TEMPLATES.put("OPHTHALMOLOGY", Arrays.asList(
                new ProcedureTemplate("Consulta oftalmológica", 30),
                new ProcedureTemplate("Control de visión", 20),
                new ProcedureTemplate("Fondo de ojo", 20),
                new ProcedureTemplate("Tonometría (presión ocular)", 15),
                new ProcedureTemplate("Adaptación de lentes de contacto", 45),
                new ProcedureTemplate("Campimetría", 30)
        ));
    }

    /**
     * Obtiene procedimientos activos de un tenant por su slug (para uso público)
     */
    @Transactional(readOnly = true)
    public List<ProcedureResponse> findAllByTenantSlug(String tenantSlug) {
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + tenantSlug));

        return procedureRepository.findByTenantIdAndActiveTrue(tenant.getId())
                .stream()
                .map(ProcedureResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> findAllByCurrentTenant() {
        log.info("=== findAllByCurrentTenant() iniciando ===");
        try {
            UUID tenantId = getCurrentTenantId();
            log.info("=== TenantId obtenido: {} ===", tenantId);
            List<Procedure> procedures = procedureRepository.findByTenantIdAndActiveTrue(tenantId);
            log.info("=== Procedures encontrados en BD: {} ===", procedures.size());
            return procedures.stream()
                    .map(ProcedureResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("=== ERROR en findAllByCurrentTenant: {} ===", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> findAllByCurrentTenantIncludingInactive() {
        UUID tenantId = getCurrentTenantId();
        return procedureRepository.findByTenantId(tenantId)
                .stream()
                .map(ProcedureResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> findBySpecialty(Long specialtyId) {
        UUID tenantId = getCurrentTenantId();
        return procedureRepository.findByTenantIdAndSpecialtyIdAndActiveTrue(tenantId, specialtyId)
                .stream()
                .map(ProcedureResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene procedimientos activos de un tenant por su slug, filtrados por especialidad (para uso público)
     */
    @Transactional(readOnly = true)
    public List<ProcedureResponse> findByTenantSlugAndSpecialty(String tenantSlug, Long specialtyId) {
        Tenant tenant = tenantRepository.findBySlugAndActive(tenantSlug, true)
                .orElseThrow(() -> new ResourceNotFoundException("Consultorio no encontrado: " + tenantSlug));

        return procedureRepository.findByTenantIdAndSpecialtyIdAndActiveTrue(tenant.getId(), specialtyId)
                .stream()
                .map(ProcedureResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProcedureResponse findById(Long id) {
        UUID tenantId = getCurrentTenantId();
        Procedure procedure = procedureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento no encontrado: " + id));
        return ProcedureResponse.fromEntity(procedure);
    }

    @Transactional
    public ProcedureResponse create(ProcedureRequest request) {
        UUID tenantId = getCurrentTenantId();

        // Verificar si ya existe un procedimiento con el mismo nombre
        if (procedureRepository.existsByTenantIdAndName(tenantId, request.getName())) {
            throw new BusinessException("Ya existe un procedimiento con el nombre: " + request.getName());
        }

        Procedure procedure = new Procedure();
        procedure.setTenantId(tenantId);
        procedure.setName(request.getName());
        procedure.setDurationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 30);

        if (request.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + request.getSpecialtyId()));
            procedure.setSpecialty(specialty);
        }

        procedure = procedureRepository.save(procedure);
        log.info("Procedimiento creado: {} (tenant: {})", procedure.getName(), tenantId);

        return ProcedureResponse.fromEntity(procedure);
    }

    @Transactional
    public ProcedureResponse update(Long id, ProcedureRequest request) {
        UUID tenantId = getCurrentTenantId();

        Procedure procedure = procedureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento no encontrado: " + id));

        // Verificar si el nuevo nombre ya existe (si cambió)
        if (!procedure.getName().equals(request.getName()) &&
            procedureRepository.existsByTenantIdAndName(tenantId, request.getName())) {
            throw new BusinessException("Ya existe un procedimiento con el nombre: " + request.getName());
        }

        procedure.setName(request.getName());
        if (request.getDurationMinutes() != null) {
            procedure.setDurationMinutes(request.getDurationMinutes());
        }

        if (request.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + request.getSpecialtyId()));
            procedure.setSpecialty(specialty);
        } else {
            procedure.setSpecialty(null);
        }

        procedure = procedureRepository.save(procedure);
        log.info("Procedimiento actualizado: {} (tenant: {})", procedure.getName(), tenantId);

        return ProcedureResponse.fromEntity(procedure);
    }

    @Transactional
    public void delete(Long id) {
        UUID tenantId = getCurrentTenantId();

        Procedure procedure = procedureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimiento no encontrado: " + id));

        // Soft delete - marcar como inactivo
        procedure.setActive(false);
        procedureRepository.save(procedure);
        log.info("Procedimiento eliminado (soft delete): {} (tenant: {})", procedure.getName(), tenantId);
    }

    @Transactional
    public List<ProcedureResponse> loadTemplate(String specialtyCode, Long specialtyId) {
        UUID tenantId = getCurrentTenantId();

        List<ProcedureTemplate> templates = PROCEDURE_TEMPLATES.get(specialtyCode.toUpperCase());
        if (templates == null) {
            throw new BusinessException("No hay plantilla disponible para la especialidad: " + specialtyCode +
                    ". Especialidades disponibles: " + String.join(", ", PROCEDURE_TEMPLATES.keySet()));
        }

        Specialty specialty = null;
        if (specialtyId != null) {
            specialty = specialtyRepository.findById(specialtyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada: " + specialtyId));
        }

        List<Procedure> createdProcedures = new ArrayList<>();
        int skipped = 0;

        for (ProcedureTemplate template : templates) {
            // Verificar si ya existe
            if (procedureRepository.existsByTenantIdAndName(tenantId, template.name)) {
                skipped++;
                continue;
            }

            Procedure procedure = new Procedure();
            procedure.setTenantId(tenantId);
            procedure.setName(template.name);
            procedure.setDurationMinutes(template.durationMinutes);
            procedure.setSpecialty(specialty);

            createdProcedures.add(procedureRepository.save(procedure));
        }

        log.info("Plantilla cargada: {} procedimientos creados, {} omitidos (ya existían) - tenant: {}",
                createdProcedures.size(), skipped, tenantId);

        return createdProcedures.stream()
                .map(ProcedureResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retorna las especialidades disponibles para carga de plantillas
     */
    public List<String> getAvailableTemplates() {
        return new ArrayList<>(PROCEDURE_TEMPLATES.keySet());
    }

    /**
     * Método de diagnóstico para verificar procedimientos y tenant
     */
    @Transactional(readOnly = true)
    public Map<String, Object> debugProcedures() {
        Map<String, Object> debug = new LinkedHashMap<>();

        // Info del tenant actual
        String tenantIdStr = TenantContext.getCurrentTenant();
        debug.put("tenantIdFromContext", tenantIdStr);

        UUID tenantId = null;
        try {
            tenantId = getCurrentTenantId();
            debug.put("tenantIdParsed", tenantId.toString());
        } catch (Exception e) {
            debug.put("tenantIdError", e.getMessage());
        }

        // Todos los procedimientos en la BD
        List<Procedure> allProcedures = procedureRepository.findAll();
        debug.put("totalProceduresInDB", allProcedures.size());

        // Detalle de cada procedimiento
        List<Map<String, Object>> procedureDetails = new ArrayList<>();
        for (Procedure p : allProcedures) {
            Map<String, Object> pd = new LinkedHashMap<>();
            pd.put("id", p.getId());
            pd.put("name", p.getName());
            pd.put("tenantId", p.getTenantId() != null ? p.getTenantId().toString() : "NULL");
            pd.put("active", p.getActive());
            pd.put("matchesCurrentTenant", tenantId != null && tenantId.equals(p.getTenantId()));
            procedureDetails.add(pd);
        }
        debug.put("allProcedures", procedureDetails);

        // Procedimientos del tenant actual (con filtro active=true)
        if (tenantId != null) {
            List<Procedure> tenantProcedures = procedureRepository.findByTenantIdAndActiveTrue(tenantId);
            debug.put("proceduresForCurrentTenant", tenantProcedures.size());

            // Con incluir inactivos
            List<Procedure> allTenantProcedures = procedureRepository.findByTenantId(tenantId);
            debug.put("proceduresForCurrentTenantIncludingInactive", allTenantProcedures.size());
        }

        return debug;
    }

    private UUID getCurrentTenantId() {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("=== getCurrentTenantId() - TenantContext.getCurrentTenant() = '{}' ===", tenantId);
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.error("=== ERROR: TenantId es null o vacío ===");
            throw new BusinessException("No se pudo determinar el tenant actual. Verifique que esté autenticado correctamente.");
        }
        try {
            UUID uuid = UUID.fromString(tenantId);
            log.info("=== UUID parseado correctamente: {} ===", uuid);
            return uuid;
        } catch (IllegalArgumentException e) {
            log.error("=== ERROR parseando UUID: {} ===", tenantId, e);
            throw new BusinessException("ID de tenant inválido: " + tenantId);
        }
    }

    // Clase auxiliar para las plantillas
    private static class ProcedureTemplate {
        final String name;
        final int durationMinutes;

        ProcedureTemplate(String name, int durationMinutes) {
            this.name = name;
            this.durationMinutes = durationMinutes;
        }
    }
}

