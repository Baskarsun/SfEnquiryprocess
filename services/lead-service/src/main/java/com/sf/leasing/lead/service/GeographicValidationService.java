package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Geographic validation service.
 * Mobile channel: warning LN3955 (save allowed).
 * All other channels: hard error LN3955 (save blocked).
 */
@Service
public class GeographicValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(GeographicValidationService.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    @PersistenceContext
    private EntityManager em;

    public boolean validatePincodeBranch(String pincode, String branchCode, Channel channel) {
        if (pincode == null || pincode.isBlank() || branchCode == null || branchCode.isBlank()) {
            return true;
        }

        Long override = (Long) em.createNativeQuery(
            "SELECT COUNT(*) FROM pincode_branch_mapping WHERE pincode = ?1 AND branch_code = ?2 AND is_explicit_override = TRUE"
        ).setParameter(1, pincode).setParameter(2, branchCode).getSingleResult();
        if (override > 0) return true;

        List<Object[]> branchRow = em.createNativeQuery(
            "SELECT segment_code, max_distance_km, latitude, longitude FROM branch_config WHERE branch_code = ?1"
        ).setParameter(1, branchCode).getResultList();

        if (branchRow.isEmpty()) {
            LOG.warn("Branch {} not found in branch_config. Skipping geographic validation.", branchCode);
            return true;
        }

        String segmentCode    = (String) branchRow.get(0)[0];
        Number maxDistanceNum = (Number) branchRow.get(0)[1];
        Number branchLat      = (Number) branchRow.get(0)[2];
        Number branchLon      = (Number) branchRow.get(0)[3];

        if ("SM".equals(segmentCode)) return true;

        List<Object[]> pincodeRow = em.createNativeQuery(
            "SELECT latitude, longitude FROM pincode_coordinates WHERE pincode = ?1"
        ).setParameter(1, pincode).getResultList();

        if (pincodeRow.isEmpty()) {
            Long mapped = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM pincode_branch_mapping WHERE pincode = ?1 AND branch_code = ?2"
            ).setParameter(1, pincode).setParameter(2, branchCode).getSingleResult();
            if (mapped > 0) return true;
            LOG.warn("Pincode {} not found in coordinates table. Cannot compute distance.", pincode);
            return handleValidationOutcome(false, pincode, branchCode, channel);
        }

        if (branchLat == null || branchLon == null || maxDistanceNum == null) {
            LOG.warn("Branch {} is missing coordinates or max_distance_km. Skipping distance check.", branchCode);
            return true;
        }

        double applicantLat  = ((Number) pincodeRow.get(0)[0]).doubleValue();
        double applicantLon  = ((Number) pincodeRow.get(0)[1]).doubleValue();
        double distanceKm    = haversineKm(applicantLat, applicantLon,
                                           branchLat.doubleValue(), branchLon.doubleValue());
        double maxDistanceKm = maxDistanceNum.doubleValue();

        LOG.debug("Geo check: pincode={} branch={} distance={:.2f}km max={:.2f}km",
            pincode, branchCode, distanceKm, maxDistanceKm);

        return handleValidationOutcome(distanceKm <= maxDistanceKm, pincode, branchCode, channel);
    }

    private boolean handleValidationOutcome(boolean withinRange, String pincode, String branchCode, Channel channel) {
        if (withinRange) return true;
        if (channel == Channel.MOBILE) {
            LOG.info("LN3955 warning (mobile): pincode={} is outside branch={} service area. Save allowed.", pincode, branchCode);
            return false;
        }
        throw new BusinessException(ErrorCodes.GEOGRAPHIC_VALIDATION,
            "LN3955: Applicant pincode " + pincode + " is outside the service area of branch " + branchCode + ".");
    }

    double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
