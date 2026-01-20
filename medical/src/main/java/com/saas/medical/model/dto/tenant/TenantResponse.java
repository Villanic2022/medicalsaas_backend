package com.saas.medical.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String timezone;
    private Integer appointmentDurationMinutes;
    private Boolean active;
}
