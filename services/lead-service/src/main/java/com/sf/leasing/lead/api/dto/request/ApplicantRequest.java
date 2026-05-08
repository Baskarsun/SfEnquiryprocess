package com.sf.leasing.lead.api.dto.request;

import java.time.LocalDate;

public class ApplicantRequest {
    public String applicantName;
    public String gender;
    public LocalDate dateOfBirth;
    public String constitutionType;   // INDIVIDUAL | NON_INDIVIDUAL
    public String residentialType;    // RESIDENT | NRI
    public String mobile;
    public String email;
    public String pan;
    public String gstin;
    public String aadhaar;
    public String passportNumber;
    public LocalDate passportValidityDate;
    public String voterId;
    public String drivingLicence;
    public String dan;
    public String occupation;
    public String addressLine1;
    public String addressLine2;
    public String pincode;
    public String locationName;
    public String city;
    public String state;
}
