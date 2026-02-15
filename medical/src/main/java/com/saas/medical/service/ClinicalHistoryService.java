package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.clinical.ClinicalNoteRequest;
import com.saas.medical.model.dto.clinical.ClinicalNoteResponse;
import com.saas.medical.model.dto.clinical.PatientFileResponse;
import com.saas.medical.model.entity.*;
import com.saas.medical.repository.*;
import com.saas.medical.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalHistoryService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final PatientFileRepository patientFileRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads/patient-files}")
    private String uploadDir;

    @Value("${app.upload.max-size:10485760}") // 10MB por defecto
    private long maxFileSize;

    // ==================== CLINICAL NOTES ====================

    @Transactional(readOnly = true)
    public List<ClinicalNoteResponse> getClinicalNotes(Long patientId) {
        UUID tenantId = getCurrentTenantId();

        // Verificar que el paciente existe y pertenece al tenant
        validatePatientAccess(patientId, tenantId);

        List<ClinicalNote> notes = clinicalNoteRepository.findByPatientIdAndTenantIdOrderByCreatedAtDesc(patientId, tenantId);

        // Obtener info del usuario actual para determinar permisos de eliminación
        Long currentProfessionalId = getCurrentProfessionalId();
        boolean isOwner = isCurrentUserOwner();

        return notes.stream()
                .map(note -> mapToNoteResponse(note, currentProfessionalId, isOwner))
                .collect(Collectors.toList());
    }

    @Transactional
    public ClinicalNoteResponse createClinicalNote(Long patientId, ClinicalNoteRequest request) {
        UUID tenantId = getCurrentTenantId();

        // Verificar que el paciente existe
        Patient patient = patientRepository.findByIdWithTenant(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", patientId));

        if (!patient.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }

        // Obtener el profesional del usuario actual
        Professional professional = getCurrentProfessional();
        if (professional == null) {
            throw new BusinessException("Solo los profesionales pueden crear evoluciones");
        }

        ClinicalNote note = new ClinicalNote();
        note.setTenantId(tenantId);
        note.setPatient(patient);
        note.setProfessional(professional);
        note.setProfessionalName(professional.getFullName());
        note.setContent(request.getContent());
        note.setActive(true);

        note = clinicalNoteRepository.save(note);
        log.info("Evolución creada - ID: {}, Paciente: {}, Profesional: {}",
                note.getId(), patientId, professional.getFullName());

        return mapToNoteResponse(note, professional.getId(), isCurrentUserOwner());
    }

    @Transactional
    public void deleteClinicalNote(Long patientId, Long noteId) {
        UUID tenantId = getCurrentTenantId();

        ClinicalNote note = clinicalNoteRepository.findByIdAndPatientIdAndTenantId(noteId, patientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Evolución", "id", noteId));

        // Verificar permisos: solo el profesional que creó la nota o el OWNER pueden eliminar
        Long currentProfessionalId = getCurrentProfessionalId();
        boolean isOwner = isCurrentUserOwner();

        if (!isOwner && (currentProfessionalId == null || !currentProfessionalId.equals(note.getProfessional().getId()))) {
            throw new BusinessException("No tiene permisos para eliminar esta evolución");
        }

        // Soft delete
        note.setActive(false);
        clinicalNoteRepository.save(note);
        log.info("Evolución eliminada (soft delete) - ID: {}", noteId);
    }

    // ==================== PATIENT FILES ====================

    @Transactional(readOnly = true)
    public List<PatientFileResponse> getPatientFiles(Long patientId) {
        UUID tenantId = getCurrentTenantId();

        // Verificar que el paciente existe y pertenece al tenant
        validatePatientAccess(patientId, tenantId);

        List<PatientFile> files = patientFileRepository.findByPatientIdAndTenantIdOrderByCreatedAtDesc(patientId, tenantId);

        Long currentUserId = getCurrentUserId();
        boolean isOwner = isCurrentUserOwner();

        return files.stream()
                .map(file -> mapToFileResponse(file, currentUserId, isOwner))
                .collect(Collectors.toList());
    }

    @Transactional
    public PatientFileResponse uploadFile(Long patientId, MultipartFile file, String description) {
        UUID tenantId = getCurrentTenantId();

        // Validar archivo
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException("El archivo excede el tamaño máximo permitido (10MB)");
        }

        // Verificar que el paciente existe
        Patient patient = patientRepository.findByIdWithTenant(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", patientId));

        if (!patient.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }

        // Obtener usuario actual
        User currentUser = getCurrentUser();

        // Generar nombre único para el archivo
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // Crear directorio si no existe
        Path uploadPath = Paths.get(uploadDir, tenantId.toString(), patientId.toString());
        try {
            Files.createDirectories(uploadPath);

            // Guardar archivo
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Crear registro en BD
            PatientFile patientFile = new PatientFile();
            patientFile.setTenantId(tenantId);
            patientFile.setPatient(patient);
            patientFile.setUploadedBy(currentUser);
            patientFile.setFileName(uniqueFileName);
            patientFile.setOriginalName(originalName);
            patientFile.setContentType(file.getContentType());
            patientFile.setFileSize(file.getSize());
            patientFile.setFilePath(filePath.toString());
            patientFile.setDescription(description);
            patientFile.setActive(true);

            patientFile = patientFileRepository.save(patientFile);
            log.info("Archivo subido - ID: {}, Paciente: {}, Archivo: {}",
                    patientFile.getId(), patientId, originalName);

            return mapToFileResponse(patientFile, currentUser.getId(), isCurrentUserOwner());

        } catch (IOException e) {
            log.error("Error al guardar archivo: {}", e.getMessage());
            throw new BusinessException("Error al guardar el archivo");
        }
    }

    @Transactional
    public void deleteFile(Long patientId, Long fileId) {
        UUID tenantId = getCurrentTenantId();

        PatientFile file = patientFileRepository.findByIdAndPatientIdAndTenantId(fileId, patientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", "id", fileId));

        // Verificar permisos: solo quien subió el archivo o el OWNER pueden eliminar
        Long currentUserId = getCurrentUserId();
        boolean isOwner = isCurrentUserOwner();

        if (!isOwner && !currentUserId.equals(file.getUploadedBy().getId())) {
            throw new BusinessException("No tiene permisos para eliminar este archivo");
        }

        // Soft delete (no eliminamos el archivo físico por seguridad)
        file.setActive(false);
        patientFileRepository.save(file);
        log.info("Archivo eliminado (soft delete) - ID: {}", fileId);
    }

    public byte[] downloadFile(Long patientId, Long fileId) {
        UUID tenantId = getCurrentTenantId();

        PatientFile file = patientFileRepository.findByIdAndPatientIdAndTenantId(fileId, patientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", "id", fileId));

        try {
            Path filePath = Paths.get(file.getFilePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Error al leer archivo: {}", e.getMessage());
            throw new BusinessException("Error al descargar el archivo");
        }
    }

    public PatientFile getFileEntity(Long patientId, Long fileId) {
        UUID tenantId = getCurrentTenantId();
        return patientFileRepository.findByIdAndPatientIdAndTenantId(fileId, patientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", "id", fileId));
    }

    // ==================== HELPER METHODS ====================

    private UUID getCurrentTenantId() {
        String tenantIdStr = TenantContext.getCurrentTenant();
        if (tenantIdStr == null) {
            throw new BusinessException("No se pudo determinar el tenant actual");
        }
        return UUID.fromString(tenantIdStr);
    }

    private void validatePatientAccess(Long patientId, UUID tenantId) {
        Patient patient = patientRepository.findByIdWithTenant(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", patientId));

        if (!patient.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Paciente", "id", patientId);
        }
    }

    private Long getCurrentProfessionalId() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .flatMap(user -> professionalRepository.findByUserId(user.getId()))
                .map(Professional::getId)
                .orElse(null);
    }

    private Professional getCurrentProfessional() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .flatMap(user -> professionalRepository.findByUserId(user.getId()))
                .orElse(null);
    }

    private User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private boolean isCurrentUserOwner() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_OWNER"));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private ClinicalNoteResponse mapToNoteResponse(ClinicalNote note, Long currentProfessionalId, boolean isOwner) {
        boolean canDelete = isOwner || (currentProfessionalId != null && currentProfessionalId.equals(note.getProfessional().getId()));

        return ClinicalNoteResponse.builder()
                .id(note.getId())
                .patientId(note.getPatient().getId())
                .professionalId(note.getProfessional().getId())
                .professionalName(note.getProfessionalName())
                .content(note.getContent())
                .date(note.getCreatedAt())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .canDelete(canDelete)
                .build();
    }

    private PatientFileResponse mapToFileResponse(PatientFile file, Long currentUserId, boolean isOwner) {
        boolean canDelete = isOwner || currentUserId.equals(file.getUploadedBy().getId());

        return PatientFileResponse.builder()
                .id(file.getId())
                .patientId(file.getPatient().getId())
                .fileName(file.getFileName())
                .originalName(file.getOriginalName())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .description(file.getDescription())
                .uploadedBy(file.getUploadedBy().getFullName())
                .createdAt(file.getCreatedAt())
                .downloadUrl("/patients/" + file.getPatient().getId() + "/files/" + file.getId() + "/download")
                .canDelete(canDelete)
                .build();
    }
}

