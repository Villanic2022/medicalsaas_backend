package com.saas.medical.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    private String password;
}
