package com.saas.medical.controller;

import com.saas.medical.model.dto.appointment.AppointmentResponse;
import com.saas.medical.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Administración de Turnos", description = "APIs para gestión de turnos - requiere autenticación")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Listar turnos", description = "Lista todos los turnos del tenant autenticado")
    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
    public ResponseEntity<List<AppointmentResponse>> getAppointments() {
        List<AppointmentResponse> appointments = appointmentService.findAppointmentsByCurrentTenant();
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener turno por ID", description = "Obtiene los detalles de un turno específico")
    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable Long id) {
        AppointmentResponse appointment = appointmentService.findByIdAndCurrentTenant(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar estado", description = "Actualiza el estado de un turno (confirmar/cancelar/completar)")
    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        AppointmentResponse appointment = appointmentService.updateStatus(id, status);
        return ResponseEntity.ok(appointment);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar turno", description = "Cancela un turno médico")
    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
