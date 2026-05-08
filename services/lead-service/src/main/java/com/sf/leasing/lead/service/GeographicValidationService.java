package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Geographic validation service — Phase 2 enhancement of the inline Phase 1 stub.
 *
 * Uses Haversine formula to compute actual distance between applicant pincode centroid
 * and branch coordinates. Rules per LP3 / LP8 / PP3:
 *   - SME branches (segment_code = 'SM') are exempt.
 *   - Users with bypass access or explicit override mapping are exempt.
 *   - Mobile channel: warning LN3955 (save allowed).
 *   - All other channels: hard error LN3955 (save blocked).
 */
@ApplicationScoped
public class GeographicValidationService {

    private static final Logger LOG = Logger.getLogger(GeographicValidationService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Inject
    EntityManager em;

    /**
     * Validate that the applicant's pincode is within the branch service area.
     *
     * @return true  → within service area (or exempt); proceed normally.
     *         false → outside service area; mobile = warning, other channels = hard error.
     * @throws BusinessException for non-mobile channels when outside service area.
     */
    public boolean validatePincodeBranch(String pincode, String branchCode, Channel channel) {
        if (pincode == null || pincode.isBlank() || branchCode == null || branchCode.isBlank()) {
            return true;
        }

        // Explicit override always passes
        Long override = (Long) em.createNativeQuery(
            "SELECT COUNT(*) FROM pincode_branch_mapping WHERE pincode = ?1 AND branch_code = ?2 AND is_explicit_override = TRUE"
        ).setParameter(1, pincode).setParameter(2, branchCode).getSingleResult();
        if (override > 0) return true;

        // SME branches are exempt from geographic validation
        List<Object[]> branchRow = em.createNativeQuery(
            "SELECT segment_code, max_distance_km, latitude, longitude FROM branch_config WHERE branch_code = ?1"
        ).setParameter(1, branchCode).getResultList();

        if (branchRow.isEmpty()) {
            LOG.warnf("Branch %s not found in branch_config. Skipping geographic validation.", branchCode);
            return true;
        }

        String segmentCode    = (String) branchRow.get(0)[0];
        Number maxDistanceNum = (Number) branchRow.get(0)[1];
        Number branchLat      = (Number) branchRow.get(0)[2];
        Number branchLon      = (Number) branchRow.get(0)[3];

        if ("SM".equals(segmentCode)) return true;

        // Pincode centroid lookup
        List<Object[]> pincodeRow = em.createNativeQuery(
            "SELECT latitude, longitude FROM pincode_coordinates WHERE pincode = ?1"
        ).setParameter(1, pincode).getResultList();

        if (pincodeRow.isEmpty()) {
            // Unknown pincode — fall back to mapped-pincode check
            Long mapped = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM pincode_branch_mapping WHERE pincode = ?1 AND branch_code = ?2"
            ).setParameter(1, pincode).setParameter(2, branchCode).getSingleResult();
            if (mapped > 0) return true;

            LOG.warnf("Pincode %s not found in coordinates table. Cannot compute distance.", pincode);
            return handleValidationOutcome(false, pincode, branchCode, channel);
        }

        if (branchLat == null || branchLon == null || maxDistanceNum == null) {
            LOG.warnf("Branch %s is missing coordinates or max_distance_km. Skipping distance check.", branchCode);
            return true;
        }

        double applicantLat  = ((Number) pincodeRow.get(0)[0]).doubleValue();
        double applicantLon  = ((Number) pincodeRow.get(0)[1]).doubleValue();
        double distanceKm    = haversineKm(applicantLat, applicantLon,
                                           branchLat.doubleValue(), branchLon.doubleValue());
        double maxDistanceKm = maxDistanceNum.doubleValue();

        LOG.debugf("Geo check: pincode=%s branch=%s distance=%.2fkm max=%.2fkm",
            pincode, branchCode, distanceKm, maxDistanceKm);

        boolean withinRange = distanceKm <= maxDistanceKm;
        return handleValidationOutcome(withinRange, pincode, branchCode, channel);
    }

    // -------------------------------------------------------
    // Private
    // -------------------------------------------------------

    private boolean handleValidationOutcome(boolean withinRange, String pincode, String branchCode, Channel channel) {
        if (withinRange) return true;
        if (channel == Channel.MOBILE) {
            LOG.infof("LN3955 warning (mobile): pincode=%s is outside branch=%s service area. Save allowed.", pincode, branchCode);
            return false;  // Caller includes warning in response; save is NOT blocked
        }
        throw new BusinessException(ErrorCodes.GEOGRAPHIC_VALIDATION,
            "LN3955: Applicant pincode " + pincode + " is outside the service area of branch " + branchCode + ".");
    }

    /**
     * Haversine great-circle distance between two lat/lon points.
     */
    double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
