package com.saas.medical.model.dto.procedure;

import com.saas.medical.model.entity.Procedure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureResponse {

    private Long id;
    private String name;
    private Integer durationMinutes;
    private Long specialtyId;
    private String specialtyName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProcedureResponse fromEntity(Procedure procedure) {
        return ProcedureResponse.builder()
                .id(procedure.getId())
                .name(procedure.getName())
                .durationMinutes(procedure.getDurationMinutes())
                .specialtyId(procedure.getSpecialty() != null ? procedure.getSpecialty().getId() : null)
                .specialtyName(procedure.getSpecialty() != null ? procedure.getSpecialty().getName() : null)
                .active(procedure.getActive())
                .createdAt(procedure.getCreatedAt())
                .updatedAt(procedure.getUpdatedAt())
                .build();
    }
}

