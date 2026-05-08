package com.sf.leasing.lead.infrastructure.validation;

import java.util.regex.Pattern;

/**
 * Validates KYC document formats per Rules LP4.3, LP8 validation checklist.
 */
public final class IdentityFormatValidator {

    private IdentityFormatValidator() {}

    private static final Pattern PAN_PATTERN    = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern GSTIN_PATTERN  = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("^[A-Z]{1}[0-9]{7}$");
    private static final Pattern VOTER_ID_PATTERN = Pattern.compile("^[A-Z]{3}[0-9]{7}$");

    public static boolean isValidPan(String pan) {
        return pan != null && PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches();
    }

    public static boolean isValidGstin(String gstin) {
        return gstin != null && GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches();
    }

    public static boolean isValidMobile(String mobile) {
        return mobile != null && MOBILE_PATTERN.matcher(mobile.trim()).matches();
    }

    public static boolean isValidPassport(String passport) {
        return passport != null && PASSPORT_PATTERN.matcher(passport.trim().toUpperCase()).matches();
    }

    public static boolean isValidVoterId(String voterId) {
        return voterId != null && VOTER_ID_PATTERN.matcher(voterId.trim().toUpperCase()).matches();
    }

    public static boolean hasMinimumIdentifier(String mobile, String email, String pan, String gstin, String address) {
        return isPresent(mobile) || isPresent(email) || isPresent(pan) || isPresent(gstin) || isPresent(address);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
