package com.sf.leasing.lead.api.dto.response;

import java.time.LocalDateTime;

public class ApiError {
    public String errorCode;
    public String message;
    public boolean warningOnly;
    public LocalDateTime timestamp = LocalDateTime.now();

    public ApiError(String errorCode, String message, boolean warningOnly) {
        this.errorCode   = errorCode;
        this.message     = message;
        this.warningOnly = warningOnly;
    }
}
