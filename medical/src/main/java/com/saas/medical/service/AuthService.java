package com.saas.medical.service;

import com.saas.medical.exception.BusinessException;
import com.saas.medical.exception.ResourceNotFoundException;
import com.saas.medical.model.dto.auth.AuthResponse;
import com.saas.medical.model.dto.auth.LoginRequest;
import com.saas.medical.model.dto.auth.RegisterRequest;
import com.saas.medical.model.entity.Role;
import com.saas.medical.model.entity.Tenant;
import com.saas.medical.model.entity.User;
import com.saas.medical.model.entity.Professional;
import com.saas.medical.repository.RoleRepository;
import com.saas.medical.repository.TenantRepository;
import com.saas.medical.repository.UserRepository;
import com.saas.medical.repository.ProfessionalRepository;
import com.saas.medical.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            CustomUserDetailsService.CustomUserPrincipal userPrincipal =
                (CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();

            User user = userPrincipal.getUser();

            // Actualizar último login
            userDetailsService.updateLastLogin(user.getEmail());

            // Obtener el rol principal (primer rol)
            String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("STAFF");

            // Generar tokens
            String tenantId = user.getTenantId() != null ? user.getTenantId().toString() : null;
            String token = jwtUtil.generateToken(user.getEmail(), tenantId, role);
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            // Obtener tenantSlug si existe tenantId
            String tenantSlug = null;
            if (user.getTenantId() != null) {
                Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
                tenantSlug = tenant != null ? tenant.getSlug() : null;
            }

            // Obtener professionalId si el usuario tiene rol PROFESSIONAL
            Long professionalId = null;
            if ("PROFESSIONAL".equals(role)) {
                Professional professional = professionalRepository.findByUserId(user.getId()).orElse(null);
                if (professional != null) {
                    professionalId = professional.getId();
                    log.info("Usuario PROFESSIONAL {} vinculado a professional ID: {}", user.getEmail(), professionalId);
                } else {
                    log.warn("Usuario PROFESSIONAL {} no tiene un registro de professional vinculado", user.getEmail());
                }
            }

            return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .tenantId(tenantId)
                    .tenantSlug(tenantSlug)
                    .role(role)
                    .active(user.getActive())
                    .professionalId(professionalId)
                    .build())
                .build();

        } catch (BadCredentialsException e) {
            log.error("Credenciales inválidas para usuario: {}", loginRequest.getEmail());
            throw new BusinessException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest, String roleName) {
        // Verificar si el email ya existe
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BusinessException("Email ya está registrado");
        }

        // Buscar el rol
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Buscar o crear tenant según el rol
        Tenant tenant = null;
        if (!"ADMIN".equals(roleName)) {
            if ("OWNER".equals(roleName)) {
                // Para OWNER: crear tenant automáticamente
                tenant = createTenantForOwner(registerRequest);
            } else {
                // Para STAFF y PROFESSIONAL: debe especificar tenantSlug
                if (registerRequest.getTenantSlug() == null) {
                    String roleDesc = "PROFESSIONAL".equals(roleName) ? "El profesional" : "El personal";
                    throw new BusinessException(roleDesc + " debe especificar a qué consultorio pertenece (tenantSlug)");
                }
                tenant = tenantRepository.findBySlugAndActive(registerRequest.getTenantSlug(), true)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + registerRequest.getTenantSlug()));
            }
        }

        // Crear usuario
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setTenantId(tenant != null ? tenant.getId() : null);
        user.setRoles(Set.of(role));
        user.setActive(true);
        user.setEmailVerified(false);

        user = userRepository.save(user);

        // Generar tokens
        String tenantId = user.getTenantId() != null ? user.getTenantId().toString() : null;
        String token = jwtUtil.generateToken(user.getEmail(), tenantId, roleName);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
            .token(token)
            .refreshToken(refreshToken)
            .user(AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tenantId(tenantId)
                .tenantSlug(tenant != null ? tenant.getSlug() : null)
                .role(roleName)
                .active(user.getActive())
                .build())
            .build();
    }

    private Tenant createTenantForOwner(RegisterRequest registerRequest) {
        // Generar slug único basado en el nombre del propietario
        String baseSlug = generateSlugFromName(registerRequest.getFirstName(), registerRequest.getLastName());
        String uniqueSlug = ensureUniqueSlug(baseSlug);

        // Determinar nombre del consultorio
        String clinicName = registerRequest.getClinicName();
        if (clinicName == null || clinicName.trim().isEmpty()) {
            clinicName = "Consultorio Dr. " + registerRequest.getFirstName() + " " + registerRequest.getLastName();
        }

        // Crear tenant
        Tenant tenant = new Tenant();
        tenant.setName(clinicName);
        tenant.setSlug(uniqueSlug);
        tenant.setEmail(registerRequest.getEmail()); // Usar email del propietario como contacto
        tenant.setPhone(registerRequest.getClinicPhone());
        tenant.setAddress(registerRequest.getClinicAddress());
        tenant.setCity(registerRequest.getClinicCity() != null ? registerRequest.getClinicCity() : "Buenos Aires");
        tenant.setTimezone("America/Argentina/Buenos_Aires");
        tenant.setAppointmentDurationMinutes(30);
        tenant.setActive(true);

        tenant = tenantRepository.save(tenant);
        log.info("Tenant auto-creado para owner: {} -> {}", registerRequest.getEmail(), tenant.getSlug());
        
        return tenant;
    }

    private String generateSlugFromName(String firstName, String lastName) {
        // Limpiar y normalizar nombres
        String slug = (firstName + "-" + lastName)
            .toLowerCase()
            .replaceAll("[áàäâ]", "a")
            .replaceAll("[éèëê]", "e")
            .replaceAll("[íìïî]", "i")
            .replaceAll("[óòöô]", "o")
            .replaceAll("[úùüû]", "u")
            .replaceAll("[ñ]", "n")
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        
        return "consultorio-" + slug;
    }

    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        
        while (tenantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        return slug;
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractUsername(refreshToken);

        if (email != null && jwtUtil.validateToken(refreshToken, email)) {
            User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("STAFF");

            String tenantId = user.getTenantId() != null ? user.getTenantId().toString() : null;
            String newToken = jwtUtil.generateToken(email, tenantId, role);
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Obtener tenantSlug si existe tenantId
            String tenantSlug = null;
            if (user.getTenantId() != null) {
                Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
                tenantSlug = tenant != null ? tenant.getSlug() : null;
            }

            // Obtener professionalId si el usuario tiene rol PROFESSIONAL
            Long professionalId = null;
            if ("PROFESSIONAL".equals(role)) {
                Professional professional = professionalRepository.findByUserId(user.getId()).orElse(null);
                if (professional != null) {
                    professionalId = professional.getId();
                }
            }

            return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .user(AuthResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .tenantId(tenantId)
                    .tenantSlug(tenantSlug)
                    .role(role)
                    .active(user.getActive())
                    .professionalId(professionalId)
                    .build())
                .build();
        }

        throw new BusinessException("Token de refresh inválido", HttpStatus.UNAUTHORIZED);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getCurrentUserInfo() {
        // Obtener el usuario autenticado del contexto de Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No hay usuario autenticado", HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("STAFF");

        String tenantId = user.getTenantId() != null ? user.getTenantId().toString() : null;

        // Obtener tenantSlug si existe tenantId
        String tenantSlug = null;
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            tenantSlug = tenant != null ? tenant.getSlug() : null;
        }

        // Obtener professionalId si el usuario tiene rol PROFESSIONAL
        Long professionalId = null;
        if ("PROFESSIONAL".equals(role)) {
            Professional professional = professionalRepository.findByUserId(user.getId()).orElse(null);
            if (professional != null) {
                professionalId = professional.getId();
            }
        }

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .role(role)
                .active(user.getActive())
                .professionalId(professionalId)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuthResponse.UserInfo> getAllStaffByTenant() {
        log.info("=== Iniciando getAllStaffByTenant ===");
        
        try {
            // Obtener usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            log.info("Usuario autenticado: {}", email);
            
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            log.info("Usuario encontrado: {}, tenantId: {}", currentUser.getEmail(), currentUser.getTenantId());
                    
            // Verificar que tenga tenantId
            if (currentUser.getTenantId() == null) {
                log.warn("Usuario {} no tiene tenantId asignado", email);
                return List.of(); // Lista vacía
            }
                    
            log.info("Buscando usuarios staff para tenant: {}", currentUser.getTenantId());
            
            // Buscar staff del mismo tenant - si no hay, devolver lista vacía
            List<User> staffUsers = userRepository.findByTenantIdAndRoleName(currentUser.getTenantId(), "STAFF");
            log.info("Query ejecutada. Usuarios encontrados: {}", staffUsers != null ? staffUsers.size() : "null");
            
            if (staffUsers == null || staffUsers.isEmpty()) {
                log.info("No se encontraron usuarios staff para tenant {}", currentUser.getTenantId());
                return List.of(); // Lista vacía
            }
            
            log.info("Procesando {} usuarios staff", staffUsers.size());
            
            // Obtener el tenant una sola vez para reutilizar el slug
            Tenant tenant = tenantRepository.findById(currentUser.getTenantId()).orElse(null);
            String tenantSlug = tenant != null ? tenant.getSlug() : null;

            // Mapear a DTO
            List<AuthResponse.UserInfo> result = staffUsers.stream()
                    .map(user -> {
                        log.debug("Procesando usuario: {}", user.getEmail());
                        
                        return AuthResponse.UserInfo.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .role(user.getRoles() != null && !user.getRoles().isEmpty() ? 
                                      user.getRoles().stream()
                                              .findFirst()
                                              .map(Role::getName)
                                              .orElse("STAFF") : "STAFF")
                                .active(user.getActive())
                                .tenantId(user.getTenantId() != null ? user.getTenantId().toString() : null)
                                .tenantSlug(tenantSlug)
                                .build();
                    })
                    .toList();
            
            log.info("=== getAllStaffByTenant completado exitosamente con {} resultados ===", result.size());
            return result;
                    
        } catch (Exception e) {
            log.error("=== ERROR en getAllStaffByTenant ===");
            log.error("Tipo de error: {}", e.getClass().getSimpleName());
            log.error("Mensaje: {}", e.getMessage());
            log.error("StackTrace: ", e);
            log.error("=== FIN ERROR ===");
            
            // En lugar de lanzar excepción, devolver lista vacía
            return List.of();
        }
    }
}


