package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.ApproveQuoteRequest;
import com.sf.leasing.lead.api.dto.request.CreateQuoteRequest;
import com.sf.leasing.lead.api.dto.request.LockUnlockQuoteRequest;
import com.sf.leasing.lead.api.dto.response.QuoteResponse;
import com.sf.leasing.lead.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quote", description = "PP6: Quote lifecycle — create, approve, share, lock")
public class QuoteResource {

    private final QuoteService quoteService;

    public QuoteResource(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * POST /api/v1/opportunities/{opportunityId}/quotes
     * PP6: Create a new quote (rack-rate or customised) for an Opportunity.
     */
    @PostMapping("/opportunities/{opportunityId}/quotes")
    @Operation(summary = "Create a quote for an opportunity (PP6.1–PP6.5)")
    public ResponseEntity<QuoteResponse> createQuote(
        @PathVariable("opportunityId") String opportunityId,
        @Valid @RequestBody CreateQuoteRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        QuoteResponse quote = quoteService.createQuote(opportunityId, req, userId);
        return ResponseEntity.status(201).body(quote);
    }

    /**
     * GET /api/v1/opportunities/{opportunityId}/quotes
     * PP6: List all quote versions for an Opportunity.
     */
    @GetMapping("/opportunities/{opportunityId}/quotes")
    @Operation(summary = "List all quote versions for an opportunity")
    public ResponseEntity<List<QuoteResponse>> listQuotes(
        @PathVariable("opportunityId") String opportunityId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        List<QuoteResponse> quotes = quoteService.listByOpportunity(opportunityId);
        return ResponseEntity.ok(quotes);
    }

    /**
     * GET /api/v1/quotes/{quoteId}
     * PP6: Get a specific quote.
     */
    @GetMapping("/quotes/{quoteId}")
    @Operation(summary = "Get quote by ID")
    public ResponseEntity<QuoteResponse> getQuote(
        @PathVariable("quoteId") String quoteId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(quoteService.getByQuoteId(quoteId));
    }

    /**
     * POST /api/v1/quotes/{quoteId}/approve
     * PP6.4: Approve or reject a customised quote.
     */
    @PostMapping("/quotes/{quoteId}/approve")
    @Operation(summary = "Approve or reject a customised quote (PP6.4)")
    public ResponseEntity<QuoteResponse> approveQuote(
        @PathVariable("quoteId") String quoteId,
        @Valid @RequestBody ApproveQuoteRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        QuoteResponse quote = quoteService.processApproval(quoteId, req, userId);
        return ResponseEntity.ok(quote);
    }

    /**
     * POST /api/v1/quotes/{quoteId}/share
     * PP6.6: Share a quote with the lessee (Draft/Approved → Shared).
     */
    @PostMapping("/quotes/{quoteId}/share")
    @Operation(summary = "Share a quote (PP6.6)")
    public ResponseEntity<QuoteResponse> shareQuote(
        @PathVariable("quoteId") String quoteId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        QuoteResponse quote = quoteService.shareQuote(quoteId, userId);
        return ResponseEntity.ok(quote);
    }

    /**
     * POST /api/v1/quotes/{quoteId}/lock
     * PP6.6: Lock, request unlock, or approve unlock of a quote.
     * Body action: LOCK | REQUEST_UNLOCK | APPROVE_UNLOCK
     */
    @PostMapping("/quotes/{quoteId}/lock")
    @Operation(summary = "Lock / unlock a quote (PP6.6–PP6.7)")
    public ResponseEntity<QuoteResponse> lockQuote(
        @PathVariable("quoteId") String quoteId,
        @RequestBody LockUnlockQuoteRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        QuoteResponse quote = quoteService.processLockAction(quoteId, req, userId);
        return ResponseEntity.ok(quote);
    }
}
