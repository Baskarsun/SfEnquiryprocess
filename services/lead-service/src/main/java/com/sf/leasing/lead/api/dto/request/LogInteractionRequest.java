package com.sf.leasing.lead.api.dto.request;

import com.sf.leasing.lead.domain.enums.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LogInteractionRequest {

    @NotNull(message = "Interaction type is mandatory")
    public InteractionType interactionType;

    @NotNull(message = "Interaction timestamp is mandatory")
    public LocalDateTime interactionTimestamp;

    @NotBlank(message = "Outcome notes are mandatory")
    public String outcomeNotes;

    public String contactPerson;
    public String contactDesignation;
    public String mode;

    // Mandatory for In-Progress leads (Rule LP6.2)
    public LocalDateTime nextActionDate;
    public String nextActionMode;
    public String nextContactPerson;
    public boolean reminderFlag = false;
}
