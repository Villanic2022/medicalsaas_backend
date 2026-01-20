package com.saas.medical.model.dto.specialty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecialtyRequest {

    @NotBlank(message = "El nombre de la especialidad es requerido")
    private String name;

    private String description;
}