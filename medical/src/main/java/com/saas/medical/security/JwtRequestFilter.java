package com.saas.medical.security;

import com.saas.medical.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        String tenantId = null;

        // JWT Token está en el formato "Bearer token". Remover Bearer palabra y obtener solo el token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwtToken);
                tenantId = jwtUtil.extractTenantId(jwtToken);

                // Set tenant context for current request
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId);
                }

            } catch (IllegalArgumentException e) {
                log.error("No se puede obtener el JWT Token");
            } catch (ExpiredJwtException e) {
                log.error("JWT Token ha expirado");
            }
        } else {
            logger.warn("JWT Token no comienza con Bearer String");
        }

        // Una vez obtenemos el token, validamos
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Si el token es válido, configuramos Spring Security para autenticar manualmente
            if (jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Después de establecer la Authentication en el contexto, especificamos
                // que el usuario actual está autenticado. Entonces pasa los filtros de Spring Security con éxito.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Limpiar el contexto del tenant al final de la request
            TenantContext.clear();
        }
    }
}
