package com.saas.medical.model.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentRequest {

    @NotNull(message = "El profesional es requerido")
    private Long professionalId;

    @NotNull(message = "La fecha y hora de inicio es requerida")
    private LocalDateTime startDateTime;

    private String notes;

    // Datos del paciente
    @NotNull(message = "Los datos del paciente son requeridos")
    private PatientInfo patient;

    @Data
    public static class PatientInfo {
        private String dni;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String insuranceName;
        private String insuranceNumber;
    }
}
