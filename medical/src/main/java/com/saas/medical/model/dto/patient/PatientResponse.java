package com.saas.medical.model.dto.patient;

import com.saas.medical.model.dto.insurance.InsuranceCompanyResponse;
import com.saas.medical.model.dto.professional.ProfessionalResponse;
import com.saas.medical.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String dni;
    private LocalDate birthDate;
    private Gender gender;
    private String email;
    private String phone;
    private String address;
    private InsuranceCompanyResponse insuranceCompany;
    private String insuranceNumber;
    private ProfessionalResponse preferredProfessional;
    private String notes;
    private LocalDateTime createdAt;
    private Boolean active;
}
