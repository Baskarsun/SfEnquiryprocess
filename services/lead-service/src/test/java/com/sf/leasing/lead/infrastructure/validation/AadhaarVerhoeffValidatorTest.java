package com.sf.leasing.lead.infrastructure.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AadhaarVerhoeffValidatorTest {

    // Known valid Aadhaar number (passes Verhoeff)
    @Test
    void validAadhaar_returnsTrue() {
        assertTrue(AadhaarVerhoeffValidator.isValid("234123412346"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123456789012",  // invalid Verhoeff checksum
        "00000000000",   // wrong length (11 digits)
        "1234567890123", // wrong length (13 digits)
        "abcdefghijkl",  // non-numeric
        ""
    })
    void invalidAadhaar_returnsFalse(String aadhaar) {
        assertFalse(AadhaarVerhoeffValidator.isValid(aadhaar));
    }

    @Test
    void nullAadhaar_returnsFalse() {
        assertFalse(AadhaarVerhoeffValidator.isValid(null));
    }
}
