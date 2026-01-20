package com.saas.medical.model.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {

    private Long id;
    private String dni;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String insuranceName;
    private String insuranceNumber;
    private LocalDateTime createdAt;
}
