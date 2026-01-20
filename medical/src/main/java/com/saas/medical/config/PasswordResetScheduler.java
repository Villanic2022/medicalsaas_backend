package com.saas.medical.config;

import com.saas.medical.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetScheduler {

    private final PasswordResetService passwordResetService;

    /**
     * Limpia tokens expirados cada día a la 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupExpiredTokens() {
        log.info("🧹 Iniciando limpieza de tokens de reset expirados...");
        passwordResetService.cleanupExpiredTokens();
        log.info("✅ Limpieza de tokens completada");
    }
}