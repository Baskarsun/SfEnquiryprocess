package com.sf.leasing.lead.api.dto.request;

import com.sf.leasing.lead.domain.enums.LeadTemperature;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * Manual temperature override request (LP7.3).
 * StatusReason (code + free text) is mandatory per business rule.
 */
public class OverrideTemperatureRequest {

    @NotNull(message = "Temperature is required")
    public LeadTemperature temperature;

    @NotBlank(message = "Reason code is mandatory for manual temperature override")
    public String reasonCode;

    @NotBlank(message = "Reason text is mandatory for manual temperature override")
    public String reasonText;
}
