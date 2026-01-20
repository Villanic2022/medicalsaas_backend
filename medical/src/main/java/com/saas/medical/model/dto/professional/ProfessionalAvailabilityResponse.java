package com.saas.medical.model.dto.professional;

import com.saas.medical.model.enums.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalAvailabilityResponse {

    private Long id;
    private DayOfWeek dayOfWeek;
    private String dayOfWeekDisplay;
    private LocalDate specificDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private Boolean active;
    private LocalDateTime createdAt;
    private Long professionalId;
    private String professionalName;
}