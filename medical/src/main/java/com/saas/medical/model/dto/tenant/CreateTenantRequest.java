package com.saas.medical.model.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200, message = "El nombre no puede tener más de 200 caracteres")
    private String name;

    @NotBlank(message = "El slug es requerido")
    @Size(max = 100, message = "El slug no puede tener más de 100 caracteres")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "El slug solo puede contener letras minúsculas, números y guiones")
    private String slug;

    @Email(message = "Email inválido")
    @Size(max = 100, message = "El email no puede tener más de 100 caracteres")
    private String email;

    @Size(max = 50, message = "El teléfono no puede tener más de 50 caracteres")
    private String phone;

    @Size(max = 500, message = "La dirección no puede tener más de 500 caracteres")
    private String address;

    @Size(max = 100, message = "La ciudad no puede tener más de 100 caracteres")
    private String city;

    @Size(max = 50, message = "La zona horaria no puede tener más de 50 caracteres")
    private String timezone = "America/Argentina/Buenos_Aires";

    private Integer appointmentDurationMinutes = 30;
}