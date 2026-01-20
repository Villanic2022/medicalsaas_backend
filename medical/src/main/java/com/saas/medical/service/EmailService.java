package com.saas.medical.service;

import com.saas.medical.model.entity.Appointment;
import com.saas.medical.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.templates.password-reset.subject}")
    private String passwordResetSubject;

    public void sendAppointmentConfirmation(Appointment appointment) {
        log.info("📧 Enviando email de confirmación de turno a: {}", appointment.getPatient().getEmail());

        // TODO: Implementar envío real de email
        // - Template de confirmación
        // - Botón "Agregar a Google Calendar"
        // - Información del consultorio
        // - Link de WhatsApp

        log.info("✅ Email de confirmación enviado (simulado)");
    }

    public void sendAppointmentCancellation(Appointment appointment) {
        log.info("📧 Enviando email de cancelación de turno a: {}", appointment.getPatient().getEmail());

        // TODO: Implementar envío real de email de cancelación

        log.info("✅ Email de cancelación enviado (simulado)");
    }

    public void sendAppointmentReminder(Appointment appointment) {
        log.info("📧 Enviando recordatorio de turno a: {}", appointment.getPatient().getEmail());

        // TODO: Implementar envío de recordatorio (24hs antes)

        log.info("✅ Recordatorio enviado (simulado)");
    }

    /**
     * Envía email con enlace para resetear contraseña
     */
    public void sendPasswordResetEmail(User user, String token) {
        try {
            log.info("📧 Enviando email de reset de contraseña a: {}", user.getEmail());

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String emailBody = buildPasswordResetEmailBody(user, resetLink);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(passwordResetSubject);
            message.setText(emailBody);

            mailSender.send(message);
            
            log.info("✅ Email de reset enviado exitosamente a: {}", user.getEmail());
            
        } catch (MailException e) {
            log.error("❌ Error enviando email de reset a {}: {}", user.getEmail(), e.getMessage());
            // Fallback: mostrar en logs para development
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            log.info("🔗 Link de reset (fallback): {}", resetLink);
            throw new RuntimeException("Error enviando email de recuperación: " + e.getMessage(), e);
        }
    }

    private String buildPasswordResetEmailBody(User user, String resetLink) {
        return String.format("""
            Hola %s %s,
            
            Has solicitado restablecer tu contraseña en MediSaaS.
            
            Para crear una nueva contraseña, haz clic en el siguiente enlace:
            %s
            
            Este enlace expirará en 1 hora por motivos de seguridad.
            
            Si no solicitaste este cambio, puedes ignorar este correo.
            
            Saludos,
            El equipo de MediSaaS
            
            ---
            Este es un mensaje automático, por favor no respondas a este correo.
            """, user.getFirstName(), user.getLastName(), resetLink);
    }

    /**
     * Envía confirmación de cambio de contraseña
     */
    public void sendPasswordChangeConfirmation(User user) {
        try {
            log.info("📧 Enviando confirmación de cambio de contraseña a: {}", user.getEmail());

            String emailBody = String.format("""
                Hola %s %s,
                
                Tu contraseña ha sido cambiada exitosamente en MediSaaS.
                
                Si no realizaste este cambio, contacta con nuestro equipo de soporte inmediatamente.
                
                Por tu seguridad, te recomendamos:
                - No compartir tu contraseña con nadie
                - Usar una contraseña única y segura
                - Cerrar sesión desde dispositivos compartidos
                
                Saludos,
                El equipo de MediSaaS
                
                ---
                Este es un mensaje automático, por favor no respondas a este correo.
                """, user.getFirstName(), user.getLastName());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Contraseña Cambiada - MediSaaS");
            message.setText(emailBody);

            mailSender.send(message);
            
            log.info("✅ Email de confirmación enviado exitosamente a: {}", user.getEmail());
            
        } catch (MailException e) {
            log.error("❌ Error enviando email de confirmación a {}: {}", user.getEmail(), e.getMessage());
            // No fallar la operación por error de email
            log.warn("⚠️  Continuando sin enviar email de confirmación");
        }
    }

    /**
     * Envía email de prueba para verificar configuración SMTP
     */
    public void sendTestEmail(String toEmail) {
        try {
            log.info("📧 Enviando email de prueba a: {}", toEmail);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Prueba de configuración SMTP - MediSaaS");
            message.setText("""
                ¡Felicidades!
                
                La configuración de email SMTP está funcionando correctamente.
                
                Ya puedes recibir emails de:
                - Recuperación de contraseñas
                - Confirmaciones de turnos
                - Recordatorios
                
                Saludos,
                El equipo técnico de MediSaaS
                """);

            mailSender.send(message);
            
            log.info("✅ Email de prueba enviado exitosamente a: {}", toEmail);
            
        } catch (MailException e) {
            log.error("❌ Error enviando email de prueba a {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Error en configuración SMTP: " + e.getMessage(), e);
        }
    }
}
