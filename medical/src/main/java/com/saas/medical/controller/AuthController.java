package com.saas.medical.controller;

import com.saas.medical.model.dto.auth.AuthResponse;
import com.saas.medical.model.dto.auth.LoginRequest;
import com.saas.medical.model.dto.auth.RegisterRequest;
import com.saas.medical.model.dto.auth.ForgotPasswordRequest;
import com.saas.medical.model.dto.auth.ResetPasswordRequest;
import com.saas.medical.model.dto.auth.PasswordResetResponse;
import com.saas.medical.service.AuthService;
import com.saas.medical.service.PasswordResetService;
import com.saas.medical.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario y devuelve tokens JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/owner")
    @Operation(summary = "Registrar propietario", 
               description = "Registra un nuevo propietario de consultorio. Automáticamente crea el tenant/consultorio.")
    public ResponseEntity<AuthResponse> registerOwner(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest, "OWNER");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/staff")
    @Operation(summary = "Registrar staff", 
               description = "Registra un nuevo miembro del staff. Requiere especificar 'tenantSlug' del consultorio.")
    public ResponseEntity<AuthResponse> registerStaff(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest, "STAFF");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Genera nuevos tokens usando refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil", description = "Obtiene la información del usuario autenticado")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser() {
        AuthResponse.UserInfo userInfo = authService.getCurrentUserInfo();
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/staff")
    @Operation(summary = "Obtener información del staff", description = "Obtiene la información del usuario staff autenticado")
    public ResponseEntity<AuthResponse.UserInfo> getStaffInfo() {
        AuthResponse.UserInfo userInfo = authService.getCurrentUserInfo();
        return ResponseEntity.ok(userInfo);
    }

    // ==================== ENDPOINTS DE RESET DE CONTRASEÑA ====================

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar reset de contraseña", 
               description = "Envía un email con enlace para resetear contraseña")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        PasswordResetResponse response = passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Resetear contraseña", 
               description = "Cambia la contraseña usando el token recibido por email")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validar token de reset", 
               description = "Verifica si un token de reset es válido")
    public ResponseEntity<PasswordResetResponse> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        
        if (isValid) {
            return ResponseEntity.ok(PasswordResetResponse.success("Token válido"));
        } else {
            return ResponseEntity.ok(PasswordResetResponse.error("Token inválido o expirado"));
        }
    }

    @PostMapping("/test-email")
    @Operation(summary = "Probar configuración de email", 
               description = "Envía un email de prueba para verificar la configuración SMTP")
    public ResponseEntity<PasswordResetResponse> testEmail(@RequestParam String email) {
        try {
            emailService.sendTestEmail(email);
            return ResponseEntity.ok(PasswordResetResponse.success("Email de prueba enviado exitosamente a: " + email));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(PasswordResetResponse.error("Error enviando email: " + e.getMessage()));
        }
    }

    @GetMapping("/reset-password")
    @Operation(summary = "Página temporal de reset", 
               description = "Página temporal para probar tokens de reset (solo para development)")
    public ResponseEntity<String> resetPasswordPage(@RequestParam String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Reset Password - MediSaaS</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                    .container { max-width: 500px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .status { padding: 15px; border-radius: 5px; margin-bottom: 20px; }
                    .valid { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }
                    .invalid { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }
                    .token { background: #f8f9fa; padding: 10px; border-radius: 4px; font-family: monospace; word-break: break-all; font-size: 12px; }
                    .form { margin-top: 20px; }
                    input[type="password"] { width: 100%%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; margin: 5px 0; }
                    button { background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
                    button:hover { background: #0056b3; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🔒 Resetear Contraseña - MediSaaS</h2>
                    <div class="status %s">
                        <strong>Estado del Token:</strong> %s
                    </div>
                    <div class="token">
                        <strong>Token:</strong> %s
                    </div>
                    %s
                    <hr>
                    <small><strong>Nota:</strong> Esta es una página temporal para development. En producción deberías usar tu frontend en React/Next.js.</small>
                </div>
            </body>
            </html>
            """.formatted(
                isValid ? "valid" : "invalid",
                isValid ? "✅ Token válido" : "❌ Token inválido o expirado", 
                token,
                isValid ? """
                    <div class="form">
                        <p><strong>Para resetear la contraseña, usa este endpoint:</strong></p>
                        <p>POST <code>/api/auth/reset-password</code></p>
                        <pre>{
  "token": "%s",
  "newPassword": "tuNuevaContrasena123!"
}</pre>
                        <p>Puedes probarlo en <a href="/api/swagger-ui.html" target="_blank">Swagger UI</a></p>
                    </div>
                    """.formatted(token) : ""
            );
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }
}
