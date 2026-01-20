package com.saas.medical.model.dto.specialty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialtyResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean active;
}
