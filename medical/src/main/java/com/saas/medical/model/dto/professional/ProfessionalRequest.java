package com.saas.medical.model.dto.professional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProfessionalRequest {

    @NotNull(message = "La especialidad es requerida")
    private Long specialtyId;

    @NotBlank(message = "El nombre es requerido")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    private String lastName;

    private String licenseNumber;

    @Email(message = "Email debe tener un formato válido")
    private String email;

    private String phone;

    private String bio;

    private BigDecimal privateConsultationPrice;

    private List<Long> acceptedInsurances;

    private Boolean active = true;

    // Campo para crear usuario asociado al profesional
    private String password;
}
