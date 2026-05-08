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
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customer & Lineage", description = "Phase 5: Customer creation, lineage, role assignment & analytics")
public class CustomerResource {

    @Inject
    CustomerCreationService customerCreationService;

    @Inject
    LineageService lineageService;

    @Inject
    RoleAssignmentService roleAssignmentService;

    @Inject
    AnalyticsService analyticsService;

    // -------------------------------------------------------
    // PP8.1: Customer creation
    // -------------------------------------------------------

    @POST
    @Path("/customers")
    @Operation(summary = "PP8.1 — Create enterprise customer (KYC + CAM gate)")
    public Response createCustomer(CreateCustomerRequest req) {
        if (req == null || req.applicationId == null || req.applicationId.isBlank()) {
            return Response.status(400).entity("{\"error\":\"applicationId is required\"}").build();
        }
        if (req.createdBy == null || req.createdBy.isBlank()) {
            return Response.status(400).entity("{\"error\":\"createdBy is required\"}").build();
        }
        Customer customer = customerCreationService.createCustomer(req.applicationId, req.createdBy);
        return Response.status(201).entity(CustomerResponse.from(customer)).build();
    }

    @GET
    @Path("/customers/{customerId}")
    @Operation(summary = "Retrieve enterprise customer by CUST-YYYY-NNNNNN")
    public Response getCustomer(@PathParam("customerId") String customerId) {
        Customer customer = customerCreationService.getByCustomerId(customerId);
        return Response.ok(CustomerResponse.from(customer)).build();
    }

    @GET
    @Path("/customers/by-prospect/{prospectId}")
    @Operation(summary = "Look up enterprise customer by Prospect business ID")
    public Response getCustomerByProspect(@PathParam("prospectId") String prospectId) {
        Customer customer = customerCreationService.getByProspectId(prospectId);
        return Response.ok(CustomerResponse.from(customer)).build();
    }

    @GET
    @Path("/customers/{customerId}/gate-status")
    @Operation(summary = "PP8.1 — Gate status: shows whether KYC+CAM conditions are met")
    public Response getGateStatus(@PathParam("customerId") String applicationId) {
        // Note: customerId here is the applicationId for pre-creation gate check
        CustomerResponse.GateStatus gs = customerCreationService.getGateStatus(applicationId);
        return Response.ok(gs).build();
    }

    // -------------------------------------------------------
    // PP8.2: Lineage
    // -------------------------------------------------------

    @GET
    @Path("/customers/{customerId}/lineage")
    @Operation(summary = "PP8.2 — Full lineage chain for a customer")
    public Response getLineageByCustomer(@PathParam("customerId") String customerId) {
        LineageResponse lineage = lineageService.getByCustomerId(customerId);
        return Response.ok(lineage).build();
    }

    @GET
    @Path("/lineage/by-lead/{lrn}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Lead LRN")
    public Response getLineageByLead(@PathParam("lrn") String lrn) {
        return Response.ok(lineageService.getByLeadLrn(lrn)).build();
    }

    @GET
    @Path("/lineage/by-prospect/{prospectId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Prospect business ID")
    public Response getLineageByProspect(@PathParam("prospectId") String prospectId) {
        return Response.ok(lineageService.getByProspectId(prospectId)).build();
    }

    @GET
    @Path("/lineage/by-application/{applicationId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Application ID")
    public Response getLineageByApplication(@PathParam("applicationId") String applicationId) {
        return Response.ok(lineageService.getByApplicationBusinessId(applicationId)).build();
    }

    @GET
    @Path("/lineage/by-opportunity/{opportunityId}")
    @Operation(summary = "PP8.2 — Lineage cross-reference by Opportunity ID")
    public Response getLineageByOpportunity(@PathParam("opportunityId") String opportunityId) {
        return Response.ok(lineageService.getByOpportunityBusinessId(opportunityId)).build();
    }

    // -------------------------------------------------------
    // PP8.3: Role assignments
    // -------------------------------------------------------

    @POST
    @Path("/customers/{customerId}/roles")
    @Operation(summary = "PP8.3 — Assign role to customer (Lessee/Dealer/Vendor/Depositor)")
    public Response assignRole(@PathParam("customerId") String customerId,
                                AssignRoleRequest req) {
        if (req == null || req.roleType == null) {
            return Response.status(400).entity("{\"error\":\"roleType is required\"}").build();
        }
        var assignment = roleAssignmentService.assignRole(customerId, req);
        return Response.status(201).entity(RoleAssignmentResponse.from(assignment)).build();
    }

    @GET
    @Path("/customers/{customerId}/roles")
    @Operation(summary = "PP8.3 — Get all role assignments for a customer")
    public Response getRoles(@PathParam("customerId") String customerId) {
        List<RoleAssignmentResponse> roles = roleAssignmentService.getRoles(customerId);
        return Response.ok(roles).build();
    }

    @PUT
    @Path("/customers/{customerId}/roles/{roleType}/complete-checklist")
    @Operation(summary = "PP8.3 — Mark role checklist complete; activates the role")
    public Response completeChecklist(@PathParam("customerId") String customerId,
                                       @PathParam("roleType") String roleType,
                                       AssignRoleRequest req) {
        String updatedBy = req != null ? req.assignedBy : "SYSTEM";
        List<String> items = req != null ? req.checklistItems : List.of();
        var assignment = roleAssignmentService.completeChecklist(customerId, roleType, items, updatedBy);
        return Response.ok(RoleAssignmentResponse.from(assignment)).build();
    }

    // -------------------------------------------------------
    // Section 8.5: Analytics (11 KPIs)
    // -------------------------------------------------------

    @GET
    @Path("/analytics/kpis")
    @Operation(summary = "Section 8.5 — Full 11-KPI analytics report")
    public Response getFullAnalytics(
        @QueryParam("periodFrom") String periodFrom,
        @QueryParam("periodTo")   String periodTo,
        @QueryParam("branchCode") String branchCode) {
        AnalyticsResponse report =
            analyticsService.computeFullReport(periodFrom, periodTo, branchCode);
        return Response.ok(report).build();
    }

    @GET
    @Path("/analytics/lead-aging")
    @Operation(summary = "KPI 1 — Lead aging distribution (4 buckets)")
    public Response getLeadAging(
        @QueryParam("periodFrom") String periodFrom,
        @QueryParam("periodTo")   String periodTo,
        @QueryParam("branchCode") String branchCode) {
        return Response.ok(analyticsService.computeLeadAging(periodFrom, periodTo, branchCode)).build();
    }

    @GET
    @Path("/analytics/conversion-rates")
    @Operation(summary = "KPI 6+7 — Lead-to-Prospect and Prospect-to-Customer conversion rates")
    public Response getConversionRates(
        @QueryParam("periodFrom") String periodFrom,
        @QueryParam("periodTo")   String periodTo,
        @QueryParam("branchCode") String branchCode) {
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
        return Response.ok(r).build();
    }

    @GET
    @Path("/analytics/closure-reasons")
    @Operation(summary = "KPI 8 — Closure reason distribution")
    public Response getClosureReasonDistribution(
        @QueryParam("periodFrom") String periodFrom,
        @QueryParam("periodTo")   String periodTo,
        @QueryParam("branchCode") String branchCode) {
        return Response.ok(
            analyticsService.computeClosureReasonDistribution(periodFrom, periodTo, branchCode)
        ).build();
    }

    @GET
    @Path("/analytics/cam-approval-rate")
    @Operation(summary = "KPI 11 — CAM approval rate")
    public Response getCamApprovalRate(
        @QueryParam("periodFrom") String periodFrom,
        @QueryParam("periodTo")   String periodTo,
        @QueryParam("branchCode") String branchCode) {
        long[] rate = analyticsService.computeCamApprovalRate(periodFrom, periodTo, branchCode);
        AnalyticsResponse r = new AnalyticsResponse();
        r.reportName         = "CAM_APPROVAL_RATE";
        r.totalCamSubmitted  = rate[0];
        r.totalCamApproved   = rate[1];
        r.camApprovalRatePercent = rate[0] > 0 ? (rate[1] * 100.0) / rate[0] : 0.0;
        return Response.ok(r).build();
    }
}
