package com.saas.medical.model.dto.professional;

import com.saas.medical.model.enums.DayOfWeek;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ProfessionalAvailabilityRequest {

    private DayOfWeek dayOfWeek;

    private LocalDate specificDate;

    @NotNull(message = "La hora de inicio es requerida")
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalTime endTime;

    @NotNull(message = "La duración del turno es requerida")
    @Min(value = 5, message = "La duración mínima del turno es 5 minutos")
    @Max(value = 120, message = "La duración máxima del turno es 120 minutos")
    private Integer slotDurationMinutes;

    private Boolean active = true;
}