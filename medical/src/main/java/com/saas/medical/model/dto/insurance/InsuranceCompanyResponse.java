package com.saas.medical.model.dto.insurance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceCompanyResponse {

    private Long id;
    private String name;
    private String code;
    private Boolean active;
}
