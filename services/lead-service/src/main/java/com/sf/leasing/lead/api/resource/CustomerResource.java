package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.AssignRoleRequest;
import com.sf.leasing.lead.api.dto.request.CreateCustomerRequest;
import com.sf.leasing.lead.api.dto.response.AnalyticsResponse;
import com.sf.leasing.lead.api.dto.response.CustomerResponse;
import com.sf.leasing.lead.api.dto.response.LineageResponse;
import com.sf.leasing.lead.api.dto.response.RoleAssignmentResponse;
import com.sf.leasing.lead.domain.model.Customer;
import com.sf.leasing.lead.service.AnalyticsService;
import com.sf.leasing.lead.service.CustomerCreationService;
import com.sf.leasing.lead.service.LineageService;
import com.sf.leasing.lead.service.RoleAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phase 5 — Customer Creation, Lineage & Downstream Integration API.
 *
 * Endpoints:
 *   POST   /customers                          — PP8.1: Create enterprise customer
 *   GET    /customers/{customerId}             — Retrieve customer record
 *   GET    /customers/{customerId}/gate-status — PP8.1: Blocked display
 *   GET    /customers/{customerId}/lineage     — PP8.2: Full lineage chain
 *   GET    /lineage/by-lead/{lrn}             — PP8.2: Cross-reference by Lead LRN
 *   GET    /lineage/by-prospect/{prospectId}  — PP8.2: Cross-reference by Prospect ID
 *   GET    /lineage/by-application/{appId}    — PP8.2: Cross-reference by Application ID
 *   GET    /lineage/by-opportunity/{oppId}    — PP8.2: Cross-reference by Opportunity ID
 *   POST   /customers/{customerId}/roles       — PP8.3: Assign role
 *   GET    /customers/{customerId}/roles       — PP8.3: Get role assignments
 *   PUT    /customers/{customerId}/roles/{roleType}/complete-checklist — PP8.3
 *   GET    /customers/by-prospect/{prospectId} — Look up customer by Prospect ID
 *   GET    /analytics/kpis                     — Section 8.5: Full 11-KPI report
 */
@RestController
@Tag(name = "Customer & Lineage", description = "Phase 5: Customer creation, lineage, role assignment & analytics")
public class CustomerResource {

    private final CustomerCreationService customerCreationService;
    private final LineageService lineageService;
    private final RoleAssignmentService roleAssignmentService;
    private final AnalyticsService analyticsService;

    public CustomerResource(CustomerCreationService customerCreationService,
                             LineageService lineageService,
                             RoleAssignmentService roleAssignmentService,
                             AnalyticsService analyticsService) {
        this.customerCreationService = customerCreationService;
        this.lineageService = lineageService;
        this.roleAssignmentService = roleAssignmentService;
        this.analyticsService = analyticsService;
    }

    // -------------------------------------------------------
    // PP8.1: Customer creation
    // -------------------------------------------------------

    @PostMapping("/customers")
    @Operation(summary = "PP8.1 — Create enterprise customer (KYC + CAM gate)")
    public ResponseEntity<?> createCustomer(@RequestBody CreateCustomerRequest req) {
        if (req == null || req.applicationId == null || req.applicationId.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"applicationId is required\"}");
        }
        if (req.createdBy == null || req.createdBy.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"createdBy is required\"}");
        }
        Customer customer = customerCreationService.createCustomer(req.applicationId, req.createdBy);
        return ResponseEntity.status(201).body(CustomerResponse.from(customer));
    }

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Retrieve enterprise customer by CUST-YYYY-NNNNNN")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable("customerId") String customerId) {
        Customer customer = customerCreationService.getByCustomerId(customerId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @GetMapping("/customers/by-prospect/{prospectId}")
    @Operation(summary = "Look up enterprise customer by Prospect business ID")
    public ResponseEntity<CustomerResponse> getCustomerByProspect(@PathVariable("prospectId") String prospectId) {
        Customer customer = customerCreationService.getByProspectId(prospectId);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }

    @GetMapping("/customers/{customerId}/gate-status")
    @Operation(summary = "PP8.1 — Gate status: shows whether KYC+CAM conditions are met")
    public ResponseEntity<CustomerResponse.GateStatus> getGateStatus(@PathVariable("customerId") String applicationId) {
        // Note: customerId here is the applicationId for pre-creation gate check
        CustomerResponse.GateStatus gs = customerCreationService.getGateStatus(applicationId);
        return ResponseEntity.ok(gs);
    }

    // -------------------------------------------------------
    // PP8.2: Lineage
    // -------------------------------------------------------

    @GetMapping("/customers/{customerId}/lineage")
    @Operation(summary = "PP8.2 — Full lineage chain for a customer")
    public ResponseEntity<LineageResponse> getLineageByCustomer(@PathVariable("customerId") String customerId) {
        LineageResponse lineage = lineageService.getByCustomerId(customerId);
        return ResponseEntity.ok(lineage);
    }

    @GetMapping("/lineage/by-lead/{lrn}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Lead LRN")
    public ResponseEntity<LineageResponse> getLineageByLead(@PathVariable("lrn") String lrn) {
        return ResponseEntity.ok(lineageService.getByLeadLrn(lrn));
    }

    @GetMapping("/lineage/by-prospect/{prospectId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Prospect business ID")
    public ResponseEntity<LineageResponse> getLineageByProspect(@PathVariable("prospectId") String prospectId) {
        return ResponseEntity.ok(lineageService.getByProspectId(prospectId));
    }

    @GetMapping("/lineage/by-application/{applicationId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Application ID")
    public ResponseEntity<LineageResponse> getLineageByApplication(@PathVariable("applicationId") String applicationId) {
        return ResponseEntity.ok(lineageService.getByApplicationBusinessId(applicationId));
    }

    @GetMapping("/lineage/by-opportunity/{opportunityId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Opportunity ID")
    public ResponseEntity<LineageResponse> getLineageByOpportunity(@PathVariable("opportunityId") String opportunityId) {
        return ResponseEntity.ok(lineageService.getByOpportunityBusinessId(opportunityId));
    }

    // -------------------------------------------------------
    // PP8.3: Role assignments
    // -------------------------------------------------------

    @PostMapping("/customers/{customerId}/roles")
    @Operation(summary = "PP8.3 — Assign role to customer (Lessee/Dealer/Vendor/Depositor)")
    public ResponseEntity<?> assignRole(@PathVariable("customerId") String customerId,
                                         @RequestBody AssignRoleRequest req) {
        if (req == null || req.roleType == null) {
            return ResponseEntity.badRequest().body("{\"error\":\"roleType is required\"}");
        }
        var assignment = roleAssignmentService.assignRole(customerId, req);
        return ResponseEntity.status(201).body(RoleAssignmentResponse.from(assignment));
    }

    @GetMapping("/customers/{customerId}/roles")
    @Operation(summary = "PP8.3 — Get all role assignments for a customer")
    public ResponseEntity<List<RoleAssignmentResponse>> getRoles(@PathVariable("customerId") String customerId) {
        List<RoleAssignmentResponse> roles = roleAssignmentService.getRoles(customerId);
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/customers/{customerId}/roles/{roleType}/complete-checklist")
    @Operation(summary = "PP8.3 — Mark role checklist complete; activates the role")
    public ResponseEntity<RoleAssignmentResponse> completeChecklist(
        @PathVariable("customerId") String customerId,
        @PathVariable("roleType") String roleType,
        @RequestBody AssignRoleRequest req) {
        String updatedBy = req != null ? req.assignedBy : "SYSTEM";
        List<String> items = req != null ? req.checklistItems : List.of();
        var assignment = roleAssignmentService.completeChecklist(customerId, roleType, items, updatedBy);
        return ResponseEntity.ok(RoleAssignmentResponse.from(assignment));
    }

    // -------------------------------------------------------
    // Section 8.5: Analytics (11 KPIs)
    // -------------------------------------------------------

    @GetMapping("/analytics/kpis")
    @Operation(summary = "Section 8.5 — Full 11-KPI analytics report")
    public ResponseEntity<AnalyticsResponse> getFullAnalytics(
        @RequestParam(value = "periodFrom", required = false) String periodFrom,
        @RequestParam(value = "periodTo", required = false)   String periodTo,
        @RequestParam(value = "branchCode", required = false) String branchCode) {
        AnalyticsResponse report =
            analyticsService.computeFullReport(periodFrom, periodTo, branchCode);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/analytics/lead-aging")
    @Operation(summary = "KPI 1 — Lead aging distribution (4 buckets)")
    public ResponseEntity<AnalyticsResponse.AgingBuckets> getLeadAging(
        @RequestParam(value = "periodFrom", required = false) String periodFrom,
        @RequestParam(value = "periodTo", required = false)   String periodTo,
        @RequestParam(value = "branchCode", required = false) String branchCode) {
        return ResponseEntity.ok(analyticsService.computeLeadAging(periodFrom, periodTo, branchCode));
    }

    @GetMapping("/analytics/conversion-rates")
    @Operation(summary = "KPI 6+7 — Lead-to-Prospect and Prospect-to-Customer conversion rates")
    public ResponseEntity<AnalyticsResponse> getConversionRates(
        @RequestParam(value = "periodFrom", required = false) String periodFrom,
        @RequestParam(value = "periodTo", required = false)   String periodTo,
        @RequestParam(value = "branchCode", required = false) String branchCode) {
        long[] leadConv     = analyticsService.computeLeadToProspectConversion(periodFrom, periodTo, branchCode);
        long[] prospectConv = analyticsService.computeProspectToCustomerConversion(periodFrom, periodTo, branchCode);

        AnalyticsResponse r = new AnalyticsResponse();
        r.reportName                       = "CONVERSION_RATES";
        r.periodFrom                       = periodFrom;
        r.periodTo                         = periodTo;
        r.totalLeadsCreated                = leadConv[0];
        r.totalLeadsPromoted               = leadConv[1];
        r.leadToProspectConversionPercent  = leadConv[0] > 0 ? (leadConv[1] * 100.0) / leadConv[0] : 0.0;
        r.totalProspects                   = prospectConv[0];
        r.totalCustomersCreated            = prospectConv[1];
        r.prospectToCustomerConversionPercent = prospectConv[0] > 0 ? (prospectConv[1] * 100.0) / prospectConv[0] : 0.0;
        return ResponseEntity.ok(r);
    }

    @GetMapping("/analytics/closure-reasons")
    @Operation(summary = "KPI 8 — Closure reason distribution")
    public ResponseEntity<?> getClosureReasonDistribution(
        @RequestParam(value = "periodFrom", required = false) String periodFrom,
        @RequestParam(value = "periodTo", required = false)   String periodTo,
        @RequestParam(value = "branchCode", required = false) String branchCode) {
        return ResponseEntity.ok(
            analyticsService.computeClosureReasonDistribution(periodFrom, periodTo, branchCode)
        );
    }

    @GetMapping("/analytics/cam-approval-rate")
    @Operation(summary = "KPI 11 — CAM approval rate")
    public ResponseEntity<AnalyticsResponse> getCamApprovalRate(
        @RequestParam(value = "periodFrom", required = false) String periodFrom,
        @RequestParam(value = "periodTo", required = false)   String periodTo,
        @RequestParam(value = "branchCode", required = false) String branchCode) {
        long[] rate = analyticsService.computeCamApprovalRate(periodFrom, periodTo, branchCode);
        AnalyticsResponse r = new AnalyticsResponse();
        r.reportName         = "CAM_APPROVAL_RATE";
        r.totalCamSubmitted  = rate[0];
        r.totalCamApproved   = rate[1];
        r.camApprovalRatePercent = rate[0] > 0 ? (rate[1] * 100.0) / rate[0] : 0.0;
        return ResponseEntity.ok(r);
    }
}
