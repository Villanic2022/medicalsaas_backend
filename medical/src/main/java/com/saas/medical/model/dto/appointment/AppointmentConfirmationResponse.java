package com.saas.medical.model.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConfirmationResponse {
    private Long appointmentId;
    private String confirmationCode;
    private LocalDateTime appointmentDateTime;
    private String patientName;
    private String professionalName;
    private String specialtyName;
    private String clinicName;
    private String status;
    private String whatsappUrl;
    private String googleCalendarUrl;

    public static AppointmentConfirmationResponse from(AppointmentResponse appointment) {
        return AppointmentConfirmationResponse.builder()
                .appointmentId(appointment.getId())
                .confirmationCode(generateConfirmationCode(appointment.getId()))
                .appointmentDateTime(appointment.getStartDateTime())
                .patientName(appointment.getPatient().getFullName())
                .professionalName(appointment.getProfessional().getFullName())
                .specialtyName(appointment.getProfessional().getSpecialtyName())
                .status(appointment.getStatus().name())
                .whatsappUrl(generateWhatsAppUrl(appointment))
                .googleCalendarUrl(generateGoogleCalendarUrl(appointment))
                .build();
    }

    private static String generateConfirmationCode(Long appointmentId) {
        return "CONF-" + String.format("%06d", appointmentId);
    }

    private static String generateWhatsAppUrl(AppointmentResponse appointment) {
        String message = String.format(
            "Turno confirmado para %s con %s el %s. Código: %s",
            appointment.getPatient().getFullName(),
            appointment.getProfessional().getFullName(),
            appointment.getStartDateTime().toString(),
            generateConfirmationCode(appointment.getId())
        );
        return "https://wa.me/?" + "text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String generateGoogleCalendarUrl(AppointmentResponse appointment) {
        String title = String.format("Turno médico - %s", appointment.getProfessional().getSpecialtyName());
        String details = String.format("Turno con %s. Código: %s",
            appointment.getProfessional().getFullName(),
            generateConfirmationCode(appointment.getId())
        );

        // Formato: YYYYMMDDTHHMMSSZ
        String startTime = appointment.getStartDateTime().toString().replaceAll("[-:]", "").replace("T", "T") + "00Z";
        String endTime = appointment.getEndDateTime().toString().replaceAll("[-:]", "").replace("T", "T") + "00Z";

        return String.format(
            "https://calendar.google.com/calendar/render?action=TEMPLATE&text=%s&dates=%s/%s&details=%s",
            java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8),
            startTime,
            endTime,
            java.net.URLEncoder.encode(details, java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
