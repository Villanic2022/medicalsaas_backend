package com.saas.medical.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {

    private String message;
    private boolean success;

    public static PasswordResetResponse success(String message) {
        return PasswordResetResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static PasswordResetResponse error(String message) {
        return PasswordResetResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}