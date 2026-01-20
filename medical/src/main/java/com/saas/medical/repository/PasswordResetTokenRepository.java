package com.saas.medical.repository;

import com.saas.medical.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Busca un token válido (no usado y no expirado)
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.token = :token AND prt.used = false AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Busca todos los tokens de un usuario
     */
    List<PasswordResetToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Marca todos los tokens de un usuario como usados
     */
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user.id = :userId")
    void markAllTokensAsUsedForUser(@Param("userId") Long userId);

    /**
     * Elimina tokens expirados
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Cuenta tokens válidos para un usuario en las últimas 24 horas
     */
    @Query("SELECT COUNT(prt) FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.createdAt > :since")
    long countRecentTokensForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}