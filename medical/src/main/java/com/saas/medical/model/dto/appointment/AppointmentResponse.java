package com.saas.medical.model.dto.appointment;

import com.saas.medical.model.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Appointment.AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;

    // Información del profesional
    private ProfessionalInfo professional;

    // Información del paciente
    private PatientInfo patient;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfessionalInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String specialtyName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private Long id;
        private String dni;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private String insuranceName;
        private String insuranceNumber;
    }
}
