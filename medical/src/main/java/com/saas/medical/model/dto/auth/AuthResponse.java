package com.saas.medical.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String tenantId;
        private String tenantSlug;
        private String role;
        private Boolean active;
    }
}
