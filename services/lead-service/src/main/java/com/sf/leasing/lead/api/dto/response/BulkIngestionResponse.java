package com.sf.leasing.lead.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Summary returned after a bulk ingestion (LP3) completes.
 */
public class BulkIngestionResponse {

    public UUID jobId;
    public String status;
    public int totalRows;
    public int successRows;
    public int failedRows;
    public List<RowResult> failures;

    public BulkIngestionResponse() {}

    public BulkIngestionResponse(UUID jobId, String status, int totalRows, int successRows, int failedRows,
                                  List<RowResult> failures) {
        this.jobId = jobId;
        this.status = status;
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.failedRows = failedRows;
        this.failures = failures;
    }

    public static class RowResult {
        public int rowNumber;
        public String errorCode;
        public String errorMessage;

        public RowResult(int rowNumber, String errorCode, String errorMessage) {
            this.rowNumber = rowNumber;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}
