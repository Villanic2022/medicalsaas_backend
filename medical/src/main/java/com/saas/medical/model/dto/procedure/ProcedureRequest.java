package com.saas.medical.model.dto.procedure;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProcedureRequest {

    @NotBlank(message = "El nombre del procedimiento es requerido")
    private String name;

    @Min(value = 5, message = "La duración mínima es de 5 minutos")
    private Integer durationMinutes = 30;

    private Long specialtyId;
}

