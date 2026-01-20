package com.saas.medical.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    private String lastName;

    // Para usuarios STAFF - deben especificar a qué consultorio pertenecen
    private String tenantSlug; 

    // Para usuarios OWNER - información del consultorio a crear
    private String clinicName; // Nombre del consultorio (ej: "Consultorio Dr. López")
    private String clinicPhone; 
    private String clinicAddress;
    private String clinicCity;
}
