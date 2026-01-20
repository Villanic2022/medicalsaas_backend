package com.saas.medical.controller;

import com.saas.medical.model.dto.auth.AuthResponse;
import com.saas.medical.service.AuthService; // <--- Usamos este servicio
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management")
@CrossOrigin(origins = "*")
public class StaffController {
    
    private final AuthService authService;
    
    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<AuthResponse.UserInfo>> getAllStaff() {
        return ResponseEntity.ok(authService.getAllStaffByTenant());
    }
}