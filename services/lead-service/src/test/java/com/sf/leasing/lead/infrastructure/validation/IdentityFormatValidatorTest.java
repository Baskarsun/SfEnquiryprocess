package com.sf.leasing.lead.infrastructure.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class IdentityFormatValidatorTest {

    // PAN
    @ParameterizedTest
    @ValueSource(strings = {"ABCDE1234F", "ZZZZZ9999Z"})
    void validPan(String pan) { assertTrue(IdentityFormatValidator.isValidPan(pan)); }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE123F", "1BCDE1234F", "ABCDE12345", ""})
    void invalidPan(String pan) { assertFalse(IdentityFormatValidator.isValidPan(pan)); }

    // GSTIN
    @Test
    void validGstin() {
        assertTrue(IdentityFormatValidator.isValidGstin("27ABCDE1234F1Z5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE1234F1Z5", "27ABCDE1234F1Z", ""})
    void invalidGstin(String g) { assertFalse(IdentityFormatValidator.isValidGstin(g)); }

    // Mobile
    @ParameterizedTest
    @ValueSource(strings = {"9876543210", "6000000000"})
    void validMobile(String m) { assertTrue(IdentityFormatValidator.isValidMobile(m)); }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "98765432", "987654321A"})
    void invalidMobile(String m) { assertFalse(IdentityFormatValidator.isValidMobile(m)); }

    // Minimum identifier
    @Test
    void minimumIdentifier_present() {
        assertTrue(IdentityFormatValidator.hasMinimumIdentifier("9876543210", null, null, null, null));
        assertTrue(IdentityFormatValidator.hasMinimumIdentifier(null, null, "ABCDE1234F", null, null));
        assertTrue(IdentityFormatValidator.hasMinimumIdentifier(null, null, null, null, "123 Main St"));
    }

    @Test
    void minimumIdentifier_allNull_returnsFalse() {
        assertFalse(IdentityFormatValidator.hasMinimumIdentifier(null, null, null, null, null));
    }

    @Test
    void minimumIdentifier_allBlank_returnsFalse() {
        assertFalse(IdentityFormatValidator.hasMinimumIdentifier("", "  ", "", "", ""));
    }
}
