package com.saas.medical.model.dto.patient;

import com.saas.medical.model.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientUpdateRequest {

    @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
    private String firstName;

    @Size(max = 100, message = "El apellido no puede tener más de 100 caracteres")
    private String lastName;

    @Size(max = 20, message = "El DNI no puede tener más de 20 caracteres")
    private String dni;

    private LocalDate birthDate;

    private Gender gender;

    @Email(message = "Email debe tener un formato válido")
    @Size(max = 255, message = "El email no puede tener más de 255 caracteres")
    private String email;

    @Size(max = 50, message = "El teléfono no puede tener más de 50 caracteres")
    private String phone;

    @Size(max = 500, message = "La dirección no puede tener más de 500 caracteres")
    private String address;

    private Long insuranceCompanyId;

    @Size(max = 50, message = "El número de seguro no puede tener más de 50 caracteres")
    private String insuranceNumber;

    private Long preferredProfessionalId;

    private String notes;
}