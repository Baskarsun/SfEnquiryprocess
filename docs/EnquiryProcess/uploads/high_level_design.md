# High-Level Design: SME Lending — Originations Flow

> Source of truth for business flows, domain event chains, and process rules.
> Full RS details: `Lead_Generation_RS_v1.0.md` and `Prospect_Creation_RS_v1.0.md`

---

## 1. End-to-End Origination Lifecycle

```
Multi-Channel Input
(Mobile / Web / Excel / API / QR Code)
        │
        ▼
┌──────────────┐   LeadCaptured      ┌─────────────────┐   ProspectValidated
│     Lead     │ ─────────────────►  │    Prospect      │ ──────────────────►
│  (L1: AR)    │   LeadQualified     │    (L1: AR)      │
└──────────────┘                     └─────────────────┘
                                              │
                                              │  CreateOpportunity
                                              ▼
                                     ┌─────────────────┐   QuoteAccepted
                                     │   Opportunity    │ ──────────────────►
                                     │  + Quote (VO)    │   OpportunityWon
                                     └─────────────────┘
                                              │
                                   InitiateApplication (parallel)
                                    ┌─────────┴──────────┐
                                    ▼                    ▼
                               KYC Process          Credit Appraisal
                               (External)            (CAM — Internal)
                                    └─────────┬──────────┘
                                              │ Both Complete
                                              ▼
                                     ┌─────────────────┐
                                     │ ApplicationCase  │   ApplicationSanctioned
                                     │   (L1: AR)       │ ──────────────────────►
                                     └─────────────────┘
                                              │
                                     PromoteToCustomer
                                              ▼
                                     ┌─────────────────┐
                                     │    Customer      │   CustomerCreated
                                     │  (ECID assigned) │ ──────────────────►
                                     └─────────────────┘
                                              │
                                   Downstream: Disbursement (E3)
                                              │
                                              ▼
                                     ┌─────────────────┐
                                     │  LeaseContract   │
                                     │ (L2 Servicing)   │
                                     └─────────────────┘
```

---

## 2. Process P1–P8: Lead Generation

| Process | Name | Key Business Rules |
|---|---|---|
| P1 | Base Lead Record Creation | Requires mobile number + asset intent. Auto-assign Temp Customer No. and Lead Reference No. |
| P2 | Lead Ingestion (Bulk / API) | Excel or API ingestion; invalid records → exception queue. Duplicate detection on Mobile/Email/PAN/GST. |
| P3 | Identity Capture & Existence Check | PAN/GST optional at Base; triggers UCIC lookup if provided. Flags as NEW/EXISTING/DUPLICATE. |
| P4 | Lead Assignment & Re-assignment | Up to 4-level hierarchy (CPU → Branch Manager → Associate). Re-assignment requires reason + audit trail. |
| P5 | Interaction Logging | Calls/messages/emails/meetings logged with timestamps. Supports next-action scheduling. |
| P6 | Lead Qualification & Status | System suggests Cold/Warm/Hot based on activity; manual override with mandatory reason. |
| P7 | Move to Prospect / Closure | Promotion requires valid PAN/GST. Closure requires reason code. Emits `LeadQualified`. |
| P8 | Dashboards & To-Do Lists | Role-based views: pipeline, aging, SLA compliance KPIs. |

**Lead states:** `NEW` → `IN_REVIEW` → (`RECYCLED` | `QUALIFIED` → Prospect)

---

## 3. Process P1–P8: Prospect Creation & Opportunity Management

| Process | Name | Key Business Rules |
|---|---|---|
| P1 | Prospect Interface — View Lead History | Role-based visibility; must show full lead lineage, interactions, base identifiers. |
| P2 | Prospect Assignment & Re-assignment | Central Team → Manager → Associate. All re-assignments audited. |
| P3 | PAN/GST Validation & Enrichment | Mandatory PAN (unique) or GSTIN. Auto-fill legal name + registered address from government API. Dedup must return Clean/Override before Opportunity creation. |
| P4 | Meeting & Interaction Management | Log meetings/calls against Prospect; support reminders and full history. |
| P5 | Opportunity Creation (per LoB) | One or more Opportunity IDs per Prospect; each tied to a Line of Business. Capture asset interest and type. |
| P6 | Quote Generation & Negotiation | Generate Rack Rate quote. Customized quotes require approval workflow. Multiple iterations tracked as `QuoteVersion`; `accepted_version_id` locked before application. |
| P7 | Application Origination (Parallel KYC & CAM) | Initiating an application triggers KYC and Credit Appraisal in parallel. Prospect UI shows real-time status of both. |
| P8 | Customer Creation & Lineage | `CustomerCreated` event emitted only when KYC=COMPLETE AND CAM=APPROVED. Full lineage preserved: Lead → Prospect → Opportunity → Quote → Application → Customer. |

**Prospect states:** `ACTIVE` → (`BLACK_LISTED` | `CONVERTED`)

---

## 4. Domain Events Chain

```
LeadCaptured
  └─► LeadQualified
        └─► ProspectValidated
              └─► QuoteAccepted
                    └─► OpportunityWon
                          └─► ApplicationSubmitted
                                └─► ApplicationSanctioned
                                      └─► CustomerCreated
                                            └─► DisbursementInitiated (E3)
                                                  └─► ContractActivated (L2)
```

---

## 5. Key Aggregates (from `ddd_model.csv`)

### Lead (Aggregate Root)
- **States:** `NEW`, `IN_REVIEW`, `RECYCLED`
- **Key VOs:** `raw_contact_info`, `initial_request`, `marketing_data`, `tax_identifiers`, `Interaction`
- **Invariant:** Must have a valid mobile number and `initial_request.amount > 0` before qualifying.
- **Commands:** `CaptureLead()`, `AssignAgent()`, `LogInteraction()`, `QualifyToProspect()`
- **Events:** `LeadCaptured`, `LeadQualified`

### Prospect (Aggregate Root)
- **States:** `ACTIVE`, `BLACK_LISTED`, `CONVERTED`
- **Key VOs:** `tax_identifiers` (PAN/GSTIN/CIN), `contact_details`, `demographics`, `address`, `FinancialProfile`, `ExistingObligation`, `DedupeCheckResult`
- **Child Entities:** `Opportunity`, `Quote` (nested in Opportunity as `QuoteVersion`)
- **Cross-boundary refs:** `source_lead_id`, `customer_ref_id`, `assigned_employee_id`
- **Invariant:** Unique PAN required. Dedupe must be `CLEAN` or `OVERRIDE` before creating an Opportunity.
- **Commands:** `ValidateIdentity()`, `CreateOpportunity()`, `GenerateQuote()`, `PromoteToCustomer()`
- **Events:** `ProspectValidated`, `QuoteAccepted`, `OpportunityWon`

### ApplicationCase (Aggregate Root)
- **States:** `DRAFT`, `UNDERWRITING`, `SANCTIONED`
- **Key VOs:** `tax_config`, `terms` (FinancialProposal), `ProposedAsset`, `TermSheetSnapshot`, `SanctionCondition`
- **Child Entities:** `FeeCharge`, `OneTimeCharge`, `Document Compliance Link`, `Stipulation`, `CreditDeviation`
- **Cross-boundary refs:** `product_ref_id`, `prospect_ref_id`

---

## 6. Role & Assignment Hierarchy

```
Central Processing Team (CPU)
        │
        ▼
  Branch Manager
        │
        ▼
  Associate / Field Officer / Call Centre
```

- Assignments tracked with timestamp, reason, and actor at every level.
- SLA timers enforced at each stage; breaches escalate upward.
- Lock timers (`lock_expiry_time`) on Lead and Prospect prevent concurrent ownership.

---

## 7. Integration Points

| Integration | Direction | Pattern | Notes |
|---|---|---|---|
| PAN/GST Government API | Outbound (sync) | Dapr invoke → ACL | Validates identity; enriches legal name + address |
| Dedupe / UCIC Lookup | Outbound (sync) | Dapr invoke | Returns CLEAN / DUPLICATE / OVERRIDE |
| Credit Appraisal (CAM) | Outbound (async) | Kafka event | `ApplicationSubmitted` triggers credit workflow |
| KYC Service | Outbound (async) | Kafka event | Parallel to CAM; both needed for `CustomerCreated` |
| Customer Master | Outbound (sync) | Dapr invoke | On `PromoteToCustomer()`; receives ECID |
| Notification Service | Outbound (async) | Kafka pub/sub | Sends updates on status changes |
| Regulatory & Compliance Engine | Inbound (async) | Kafka consumer | Subscribes to all events; writes immutable audit log |
| Disbursement (E3) | Outbound (async) | Kafka event | Triggered after `ApplicationSanctioned` |
