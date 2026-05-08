package com.sf.leasing.lead.infrastructure.validation;

/**
 * Validates an Aadhaar number using the Verhoeff check-digit algorithm.
 * Rule LP4.1: Must be exactly 12 digits and pass the Verhoeff computation (result = 0).
 */
public final class AadhaarVerhoeffValidator {

    private AadhaarVerhoeffValidator() {}

    // Verhoeff multiplication table
    private static final int[][] D = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
        {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
        {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
        {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
        {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
        {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
        {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
        {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
        {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    // Verhoeff permutation table
    private static final int[][] P = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
        {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
        {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
        {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
        {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
        {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
        {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    public static boolean isValid(String aadhaar) {
        if (aadhaar == null || aadhaar.length() != 12 || !aadhaar.matches("\\d{12}")) {
            return false;
        }
        int c = 0;
        String reversed = new StringBuilder(aadhaar).reverse().toString();
        for (int i = 0; i < reversed.length(); i++) {
            c = D[c][P[i % 8][reversed.charAt(i) - '0']];
        }
        return c == 0;
    }
}
