package com.sf.leasing.lead.api.dto.response;

public class CreateLeadResponse {

    public boolean success;
    public boolean routedToExceptionQueue;
    public String lrn;
    public String tempCustomerNumber;
    public String message;
    public String warning;

    public static CreateLeadResponse success(String lrn, String tempCustomerNumber) {
        CreateLeadResponse r = new CreateLeadResponse();
        r.success             = true;
        r.routedToExceptionQueue = false;
        r.lrn                 = lrn;
        r.tempCustomerNumber  = tempCustomerNumber;
        r.message             = "Lead created successfully.";
        return r;
    }

    public static CreateLeadResponse routedToExceptionQueue() {
        CreateLeadResponse r = new CreateLeadResponse();
        r.success                = false;
        r.routedToExceptionQueue = true;
        r.message = "Lead record routed to Exception Queue — minimum identifier requirement not met.";
        return r;
    }

    public CreateLeadResponse withWarning(String warning) {
        this.warning = warning;
        return this;
    }
}
