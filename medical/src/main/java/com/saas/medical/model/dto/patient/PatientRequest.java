package com.saas.medical.model.dto.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientRequest {

    @NotBlank(message = "El DNI es requerido")
    private String dni;

    @NotBlank(message = "El nombre es requerido")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    private String lastName;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email debe tener un formato válido")
    private String email;

    @NotBlank(message = "El teléfono es requerido")
    private String phone;

    @NotBlank(message = "La cobertura es requerida")
    private String insuranceName; // "Particular", "OSDE", etc.

    private String insuranceNumber; // Solo si no es "Particular"
}
