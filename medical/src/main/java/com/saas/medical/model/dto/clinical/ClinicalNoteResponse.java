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
public class ClinicalNoteResponse {

    private Long id;
    private Long patientId;
    private Long professionalId;
    private String professionalName;
    private String content;
    private LocalDateTime date;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean canDelete; // true si el usuario actual puede eliminar esta nota
}

