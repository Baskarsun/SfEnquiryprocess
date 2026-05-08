package com.sf.leasing.lead.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Haversine formula in GeographicValidationService.
 * DB-dependent path tests are covered by integration tests.
 */
class GeographicValidationServiceTest {

    private final GeographicValidationService service = new GeographicValidationService();

    @Test
    void haversineShouldReturnZeroForSamePoint() {
        double dist = service.haversineKm(19.076, 72.877, 19.076, 72.877);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    void haversineShouldComputeKnownDistance() {
        // Mumbai (~19.076°N, 72.877°E) → Delhi (~28.614°N, 77.209°E) ≈ 1150 km
        double dist = service.haversineKm(19.076, 72.877, 28.614, 77.209);
        assertTrue(dist > 1100 && dist < 1250,
            "Expected Mumbai-Delhi distance ~1150 km, got: " + dist);
    }

    @Test
    void haversineShouldBeCommutative() {
        double d1 = service.haversineKm(19.076, 72.877, 28.614, 77.209);
        double d2 = service.haversineKm(28.614, 77.209, 19.076, 72.877);
        assertEquals(d1, d2, 0.001);
    }

    @Test
    void haversineShouldReturnPositiveForDifferentPoints() {
        double dist = service.haversineKm(12.971, 77.594, 13.082, 80.270);
        assertTrue(dist > 0);
    }
}
