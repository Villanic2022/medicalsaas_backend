package com.saas.medical.model.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSearchResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String dni;
    private String phone;
    private String email;
}