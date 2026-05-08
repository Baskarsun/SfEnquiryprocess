package com.sf.leasing.lead.api.dto.request;

/**
 * Trigger a deduplication check on an existing lead (LP4).
 * Used when dedup needs to be re-run manually, e.g. after exception resolution.
 */
public class RunDedupRequest {

    /**
     * When true, the caution list, UCIC, and external PAN dedup checks are included.
     * When false (default), only the internal database dedup runs.
     */
    public boolean includeExternalChecks = true;
}
