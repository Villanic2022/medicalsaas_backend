package com.saas.medical.model.dto.clinical;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClinicalNoteRequest {

    @NotBlank(message = "El contenido de la evolución es requerido")
    private String content;
}

