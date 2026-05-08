package com.sf.leasing.lead.domain.exception;

public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final boolean warningOnly;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.warningOnly = false;
    }

    public BusinessException(String errorCode, String message, boolean warningOnly) {
        super(message);
        this.errorCode = errorCode;
        this.warningOnly = warningOnly;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isWarningOnly() {
        return warningOnly;
    }
}
