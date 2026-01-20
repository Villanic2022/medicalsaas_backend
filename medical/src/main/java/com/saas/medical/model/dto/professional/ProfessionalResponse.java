package com.saas.medical.model.dto.professional;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String licenseNumber;
    private String email;
    private String phone;
    private String bio;
    private Boolean active;
    private BigDecimal privateConsultationPrice;
    private List<InsuranceCompanyInfo> acceptedInsurances;
    private SpecialtyInfo specialty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceCompanyInfo {
        private Long id;
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialtyInfo {
        private Long id;
        private String name;
        private String description;
    }
}
