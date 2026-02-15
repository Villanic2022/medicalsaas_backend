package com.saas.medical.model.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientFileResponse {

    private Long id;
    private Long patientId;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String description;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private String downloadUrl;
    private Boolean canDelete; // true si el usuario actual puede eliminar este archivo
}

