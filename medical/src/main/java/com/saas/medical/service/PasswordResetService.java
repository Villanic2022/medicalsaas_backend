package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.auth.ForgotPasswordRequest;
import com.saas.medical.model.dto.auth.PasswordResetResponse;
import com.saas.medical.model.dto.auth.ResetPasswordRequest;
import com.saas.medical.model.entity.PasswordResetToken;
import com.saas.medical.model.entity.User;
import com.saas.medical.repository.PasswordResetTokenRepository;
import com.saas.medical.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.password.reset.token.expiration:3600}") // 1 hora por defecto
    private int tokenExpirationSeconds;

    @Value("${app.password.reset.max.attempts:5}") // 5 intentos por día
    private int maxResetAttemptsPerDay;

    /**
     * Solicita un reset de contraseña
     */
    @Transactional
    public PasswordResetResponse requestPasswordReset(ForgotPasswordRequest request) {
        try {
            // Buscar el usuario por email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElse(null); // No revelamos si el email existe o no por seguridad

            if (user != null && user.getActive()) {
                // Verificar límite de intentos
                if (hasExceededResetLimit(user.getId())) {
                    log.warn("Usuario {} ha excedido el límite de intentos de reset", user.getEmail());
                    throw new BusinessException("Se ha excedido el límite de intentos de reset. Intente nuevamente más tarde.");
                }

                // Invalidar tokens existentes
                passwordResetTokenRepository.markAllTokensAsUsedForUser(user.getId());

                // Generar nuevo token
                String token = generateSecureToken();
                LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenExpirationSeconds);

                PasswordResetToken resetToken = new PasswordResetToken(user, token, expiresAt);
                passwordResetTokenRepository.save(resetToken);

                // Enviar email
                emailService.sendPasswordResetEmail(user, token);

                log.info("Token de reset enviado para usuario: {}", user.getEmail());
            } else {
                log.info("Intento de reset para email no registrado o inactivo: {}", request.getEmail());
            }

            // Siempre devolver la misma respuesta por seguridad
            return PasswordResetResponse.success(
                "Si el email está registrado, recibirás un enlace para resetear tu contraseña.");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al procesar solicitud de reset para email: {}", request.getEmail(), e);
            throw new BusinessException("Error al procesar la solicitud. Intente nuevamente.");
        }
    }

    /**
     * Resetea la contraseña usando el token
     */
    @Transactional
    public PasswordResetResponse resetPassword(ResetPasswordRequest request) {
        try {
            // Validar que las contraseñas coincidan
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new BusinessException("Las contraseñas no coinciden");
            }

            // Buscar token válido
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findValidToken(request.getToken(), LocalDateTime.now())
                    .orElseThrow(() -> new BusinessException("Token inválido o expirado"));

            User user = resetToken.getUser();

            // Actualizar contraseña
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Marcar token como usado
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            // Invalidar todos los otros tokens del usuario
            passwordResetTokenRepository.markAllTokensAsUsedForUser(user.getId());

            // Enviar confirmación por email
            emailService.sendPasswordChangeConfirmation(user);

            log.info("Contraseña reseteada exitosamente para usuario: {}", user.getEmail());

            return PasswordResetResponse.success("Contraseña actualizada exitosamente");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al resetear contraseña", e);
            throw new BusinessException("Error al resetear la contraseña. Intente nuevamente.");
        }
    }

    /**
     * Valida si un token es válido
     */
    public boolean isTokenValid(String token) {
        try {
            return passwordResetTokenRepository
                    .findValidToken(token, LocalDateTime.now())
                    .isPresent();
        } catch (Exception e) {
            log.error("Error al validar token", e);
            return false;
        }
    }

    /**
     * Limpia tokens expirados
     */
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.debug("Tokens expirados eliminados");
        } catch (Exception e) {
            log.error("Error al limpiar tokens expirados", e);
        }
    }

    /**
     * Verifica si el usuario ha excedido el límite de intentos
     */
    private boolean hasExceededResetLimit(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        long recentAttempts = passwordResetTokenRepository.countRecentTokensForUser(userId, since);
        return recentAttempts >= maxResetAttemptsPerDay;
    }

    /**
     * Genera un token seguro
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}