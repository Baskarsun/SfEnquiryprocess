package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.ApplicantRequest;
import com.sf.leasing.lead.api.dto.request.BulkIngestRequest;
import com.sf.leasing.lead.api.dto.request.CreateLeadRequest;
import com.sf.leasing.lead.api.dto.response.BulkIngestionResponse;
import com.sf.leasing.lead.api.dto.response.CreateLeadResponse;
import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.enums.SourceCategory;
import com.sf.leasing.lead.domain.model.BulkUploadJob;
import com.sf.leasing.lead.domain.model.BulkUploadRow;
import com.sf.leasing.lead.infrastructure.persistence.BulkUploadJobRepository;
import com.sf.leasing.lead.infrastructure.persistence.BulkUploadRowRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements LP3: Bulk Upload & API Ingestion.
 *
 * LP3.1 — Excel upload: parse approved template, validate each row.
 * LP3.2 — API ingestion: schema validation, same LRN generation logic.
 * LP3.3 — Chunked batch processing with configurable batch size.
 * LP3.4 — All ingested leads default to CPU; no unassigned records.
 *
 * Excel template columns (row 1 = headers, row 2+ = data):
 *   A: Lead Type (INDIVIDUAL|COMMERCIAL)
 *   B: Source Category (INTERNAL|EXTERNAL|CAMPAIGN|DEALER)
 *   C: Source Name
 *   D: Company Known As
 *   E: Contact Person
 *   F: Applicant Name
 *   G: Mobile
 *   H: Email
 *   I: PAN
 *   J: GSTIN
 *   K: Address Line 1
 *   L: Pincode
 *   M: City
 *   N: State
 */
@Service
public class BulkIngestionService {

    private static final Logger LOG = LoggerFactory.getLogger(BulkIngestionService.class);

    @Value("${bulk.batch-size:100}")
    int batchSize;

    private final LeadCreationService leadCreationService;
    private final BulkUploadJobRepository bulkUploadJobRepository;
    private final BulkUploadRowRepository bulkUploadRowRepository;

    public BulkIngestionService(LeadCreationService leadCreationService,
                                BulkUploadJobRepository bulkUploadJobRepository,
                                BulkUploadRowRepository bulkUploadRowRepository) {
        this.leadCreationService = leadCreationService;
        this.bulkUploadJobRepository = bulkUploadJobRepository;
        this.bulkUploadRowRepository = bulkUploadRowRepository;
    }

    // -------------------------------------------------------
    // LP3.1: Excel file upload
    // -------------------------------------------------------

    // No outer transaction — each row's createLead() starts its own REQUIRED transaction;
    // failed rows roll back independently without affecting the job record or other rows.
    public BulkIngestionResponse ingestFromExcel(InputStream excelStream, String userId) {
        BulkUploadJob job = createJob("EXCEL", userId);
        List<BulkIngestionResponse.RowResult> failures = new ArrayList<>();
        int successCount = 0;
        int rowIndex = 0;

        try (Workbook workbook = new XSSFWorkbook(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            job.totalRows = Math.max(0, lastRow);  // row 0 is header
            job.status = "IN_PROGRESS";

            List<Row> batch = new ArrayList<>(batchSize);

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                batch.add(row);
                rowIndex = r;

                if (batch.size() >= batchSize || r == lastRow) {
                    for (Row batchRow : batch) {
                        BulkIngestionResponse.RowResult result = processExcelRow(batchRow, batchRow.getRowNum() + 1, job, userId);
                        if (result != null) {
                            failures.add(result);
                            job.failedRows++;
                        } else {
                            successCount++;
                            job.successRows++;
                        }
                        job.processedRows++;
                    }
                    batch.clear();
                }
            }
        } catch (Exception e) {
            LOG.error("Excel ingestion failed at row {}: {}", rowIndex, e.getMessage());
            job.status = "FAILED";
            return new BulkIngestionResponse(job.id, "FAILED", job.totalRows,
                successCount, job.failedRows, failures);
        }

        job.status = "COMPLETED";
        job.completedAt = LocalDateTime.now();
        LOG.info("Bulk Excel job {} complete: total={} success={} failed={}",
            job.id, job.totalRows, job.successRows, job.failedRows);
        return new BulkIngestionResponse(job.id, "COMPLETED", job.totalRows,
            successCount, job.failedRows, failures);
    }

    // -------------------------------------------------------
    // LP3.2: API ingestion (list of structured records)
    // -------------------------------------------------------

    public BulkIngestionResponse ingestFromApi(List<BulkIngestRequest> records, String userId) {
        BulkUploadJob job = createJob("API", userId);
        job.totalRows = records.size();
        job.status = "IN_PROGRESS";

        List<BulkIngestionResponse.RowResult> failures = new ArrayList<>();
        int successCount = 0;

        List<BulkIngestRequest> batch = new ArrayList<>(batchSize);
        int rowNum = 0;

        for (BulkIngestRequest record : records) {
            rowNum++;
            batch.add(record);

            if (batch.size() >= batchSize || rowNum == records.size()) {
                for (int i = 0; i < batch.size(); i++) {
                    int absoluteRow = rowNum - batch.size() + i + 1;
                    BulkIngestionResponse.RowResult result = processApiRecord(batch.get(i), absoluteRow, job, userId);
                    if (result != null) {
                        failures.add(result);
                        job.failedRows++;
                    } else {
                        successCount++;
                        job.successRows++;
                    }
                    job.processedRows++;
                }
                batch.clear();
            }
        }

        job.status = "COMPLETED";
        job.completedAt = LocalDateTime.now();
        LOG.info("Bulk API job {} complete: total={} success={} failed={}",
            job.id, job.totalRows, job.successRows, job.failedRows);
        return new BulkIngestionResponse(job.id, "COMPLETED", job.totalRows,
            successCount, job.failedRows, failures);
    }

    // -------------------------------------------------------
    // Private — row processing (REQUIRES_NEW so each row is independent)
    // -------------------------------------------------------

    private BulkIngestionResponse.RowResult processExcelRow(Row row, int rowNumber,
                                                              BulkUploadJob job, String userId) {
        String rawData = null;
        try {
            CreateLeadRequest req = mapExcelRowToRequest(row);
            rawData = buildRawRowData(row);

            // LP1: Validate user (device is null for bulk channel — legacy mode)
            leadCreationService.validateUserAndDevice(userId, null, null);

            // LP2: Create lead via bulk channel
            CreateLeadResponse response = leadCreationService.createLead(req, userId, Channel.BULK, null, null);

            persistRowResult(job, rowNumber, response.lrn, "SUCCESS", null, null, rawData);
            return null;  // null = success

        } catch (Exception e) {
            LOG.warn("Bulk row {} failed: {}", rowNumber, e.getMessage());
            String errorCode = extractErrorCode(e);
            persistRowResult(job, rowNumber, null, "FAILED", errorCode, e.getMessage(), rawData);
            return new BulkIngestionResponse.RowResult(rowNumber, errorCode, e.getMessage());
        }
    }

    private BulkIngestionResponse.RowResult processApiRecord(BulkIngestRequest record, int rowNumber,
                                                               BulkUploadJob job, String userId) {
        try {
            CreateLeadRequest req = mapBulkRequestToCreateRequest(record);
            leadCreationService.validateUserAndDevice(userId, null, null);
            CreateLeadResponse response = leadCreationService.createLead(req, userId, Channel.BULK, null, null);
            persistRowResult(job, rowNumber, response.lrn, "SUCCESS", null, null, record.toString());
            return null;
        } catch (Exception e) {
            LOG.warn("API bulk record {} failed: {}", rowNumber, e.getMessage());
            String errorCode = extractErrorCode(e);
            persistRowResult(job, rowNumber, null, "FAILED", errorCode, e.getMessage(), record.toString());
            return new BulkIngestionResponse.RowResult(rowNumber, errorCode, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------

    private CreateLeadRequest mapExcelRowToRequest(Row row) {
        CreateLeadRequest req = new CreateLeadRequest();
        req.leadType       = parseLeadType(cellString(row, 0));
        req.sourceCategory = parseSourceCategory(cellString(row, 1));
        req.sourceName     = cellString(row, 2);
        req.companyKnownAs = cellString(row, 3);
        req.contactPerson  = cellString(row, 4);

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.applicantName = cellString(row, 5);
        applicant.mobile        = cellString(row, 6);
        applicant.email         = cellString(row, 7);
        applicant.pan           = cellString(row, 8);
        applicant.gstin         = cellString(row, 9);
        applicant.addressLine1  = cellString(row, 10);
        applicant.pincode       = cellString(row, 11);
        applicant.city          = cellString(row, 12);
        applicant.state         = cellString(row, 13);

        req.applicants = List.of(applicant);
        return req;
    }

    private CreateLeadRequest mapBulkRequestToCreateRequest(BulkIngestRequest record) {
        CreateLeadRequest req = new CreateLeadRequest();
        req.leadType       = record.leadType;
        req.sourceCategory = record.sourceCategory;
        req.sourceName     = record.sourceName;
        req.companyKnownAs = record.companyKnownAs;
        req.contactPerson  = record.contactPerson;
        req.applicants     = record.applicants;
        return req;
    }

    // -------------------------------------------------------
    // Persistence helpers
    // -------------------------------------------------------

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    BulkUploadJob createJob(String jobType, String userId) {
        BulkUploadJob job = new BulkUploadJob();
        job.jobType   = jobType;
        job.status    = "PENDING";
        job.createdBy = userId;
        job.createdAt = LocalDateTime.now();
        return bulkUploadJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistRowResult(BulkUploadJob job, int rowNumber, String lrn,
                          String status, String errorCode, String errorMessage, String rawData) {
        BulkUploadRow rowRecord = new BulkUploadRow();
        rowRecord.job          = job;
        rowRecord.rowNumber    = rowNumber;
        rowRecord.lrn          = lrn;
        rowRecord.status       = status;
        rowRecord.errorCode    = errorCode;
        rowRecord.errorMessage = errorMessage;
        rowRecord.rawData      = rawData;
        bulkUploadRowRepository.save(rowRecord);
    }

    // -------------------------------------------------------
    // Utility
    // -------------------------------------------------------

    private String cellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        String val = cell.getCellType() == CellType.NUMERIC
            ? String.valueOf((long) cell.getNumericCellValue())
            : cell.toString();
        return val == null || val.isBlank() ? null : val.trim();
    }

    private LeadType parseLeadType(String value) {
        if (value == null) return null;
        try { return LeadType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private SourceCategory parseSourceCategory(String value) {
        if (value == null) return null;
        try { return SourceCategory.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private String extractErrorCode(Exception e) {
        if (e instanceof com.sf.leasing.lead.domain.exception.BusinessException be) {
            return be.getErrorCode();
        }
        return "SYSTEM_ERROR";
    }

    private String buildRawRowData(Row row) {
        StringBuilder sb = new StringBuilder("[");
        for (int c = 0; c < 14; c++) {
            if (c > 0) sb.append(",");
            sb.append("\"").append(cellString(row, c) != null ? cellString(row, c) : "").append("\"");
        }
        return sb.append("]").toString();
    }
}
