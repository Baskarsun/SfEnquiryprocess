package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Lead closure request (LP8 — closure sub-flow).
 * ClosureReason is mandatory; record is locked after closure.
 */
public class CloseLeadRequest {

    @NotBlank(message = "Closure reason code is mandatory")
    public String closureReasonCode;

    public String closureReasonText;
}
