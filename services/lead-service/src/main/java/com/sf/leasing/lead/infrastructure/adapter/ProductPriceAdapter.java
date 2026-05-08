package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * PP6.1: Calls the external Product Model Price Service to fetch NDLP (Net Dealer List Price).
 * If the submitted asset cost differs from NDLP, the cost is overridden and an advisory
 * message is returned to the caller.
 */
@ApplicationScoped
public class ProductPriceAdapter {

    private static final Logger LOG = Logger.getLogger(ProductPriceAdapter.class);

    @ConfigProperty(name = "adapters.product-price.stub-mode", defaultValue = "true")
    boolean stubMode;

    /**
     * Fetches the NDLP for the given make/model/year.
     * Returns Optional.empty() if the service is unavailable or no price exists.
     */
    public Optional<BigDecimal> fetchNdlp(String assetMake, String assetModel, Integer assetYear) {
        if (stubMode) {
            LOG.debugf("ProductPriceAdapter stub: returning stub NDLP for %s %s %d",
                assetMake, assetModel, assetYear);
            // Stub returns a deterministic price based on make+model hash to allow testing
            int hash = ((assetMake != null ? assetMake : "") + (assetModel != null ? assetModel : "")).hashCode();
            long basePrice = 500_000L + (Math.abs(hash) % 2_000_000L);
            return Optional.of(BigDecimal.valueOf(basePrice));
        }
        try {
            // TODO: wire to real Product Price Service REST call
            // GET /api/v1/product-price?make={make}&model={model}&year={year}
            LOG.infof("ProductPriceAdapter: fetching NDLP for %s %s %d", assetMake, assetModel, assetYear);
            return Optional.empty();
        } catch (Exception e) {
            LOG.warnf("ProductPriceAdapter: service unavailable (%s); continuing without NDLP check",
                e.getMessage());
            return Optional.empty();
        }
    }
}
