# Functional Specification — Leasing Enquiry Generation Process

**Document Version:** 1.0  
**Date:** 24/04/2026  
**Prepared By:** Analysis of LOS DB Source, ADF BC/UI Source, Lead Generation RS v1.0, Prospect Creation RS v1.0, Pricing & Quote RS v1.0, AML Process Requirements  
**Status:** Draft

**Source Documents Analysed:**
- `FS_Enquiry_Creation_Process.md` — Loan Enquiry Generation FS (lending baseline)
- `PS_PK_LN_LOAN_ENQUIRY_GEN` (.pks/.pkb — ~40,819 lines)
- `PS_PK_COM_VAL_CONTACT` (.pks/.pkb — ~19,204 lines)
- `PS_PK_LN_CAUTION_CUST_DB` (.pks/.pkb — ~11,537 lines)
- `Lead_Generation_RS_v1.0.docx` — Leasing Lead Generation Business Requirements
- `Prospect_Creation_RS_v1.0.docx` — Leasing Prospect Creation & Opportunity Management BRD
- `Pricing_Quote_RS_v1.0.docx` — Leasing Pricing & Quote Management BRD
- `sanction screening name logic and observation.docx` — AML Sanction Screening Rules

---

## Table of Contents

1. [Purpose and Scope](#1-purpose-and-scope)
2. [Architecture and System Components](#2-architecture-and-system-components)
3. [End-to-End Leasing Origination Lifecycle](#3-end-to-end-leasing-origination-lifecycle)
4. [Stage 1 — Lead Generation](#4-stage-1--lead-generation)
5. [Stage 2 — Enquiry (Prospect) Creation](#5-stage-2--enquiry-prospect-creation)
6. [Identity Validation — PAN and GSTIN](#6-identity-validation--pan-and-gstin)
7. [AML Checks](#7-aml-checks)
8. [Stage 3 — Opportunity Management](#8-stage-3--opportunity-management)
9. [Stage 4 — Pricing and Quote Management](#9-stage-4--pricing-and-quote-management)
10. [Stage 5 — Application Origination](#10-stage-5--application-origination)
11. [Stage 6 — Customer Creation and Lineage](#11-stage-6--customer-creation-and-lineage)
12. [Contact and Applicant Validation Rules](#12-contact-and-applicant-validation-rules)
13. [Caution List Screening](#13-caution-list-screening)
14. [Enquiry (Prospect) State Machine](#14-enquiry-prospect-state-machine)
15. [Key Data Structures](#15-key-data-structures)
16. [Error Codes and Messages](#16-error-codes-and-messages)
17. [Integration Catalogue](#17-integration-catalogue)
18. [Special Scenarios and Business Rules](#18-special-scenarios-and-business-rules)
19. [Audit and Logging](#19-audit-and-logging)
20. [Configuration Dependencies](#20-configuration-dependencies)
21. [Business Requirements Traceability](#21-business-requirements-traceability)

---

## 1. Purpose and Scope

The Leasing Enquiry Generation Process is the entry point for the Sundaram Finance Leasing Loan Origination System (LOS). It captures and qualifies a prospective lessee's intent, registers all applicant identity and compliance data, performs mandatory AML and regulatory screening, generates a system-tracked Enquiry (Prospect) identifier, and positions the record for downstream Credit Appraisal (CAM), KYC, Opportunity creation, and Pricing & Quote workflows.

The Leasing Enquiry process is richer than the equivalent Lending Enquiry process. In lending, a single enquiry creation step covers all compliance gates. In leasing, the process is structured as a multi-stage origination funnel:

```
Lead → Enquiry (Prospect) → Opportunity → Quote → Application → Customer Creation → Contract
```

The "Enquiry" in leasing corresponds to what is termed the **Prospect** stage in the BRD. The BRD documents use the notation **Enquiry(Prospect)** to indicate that the Prospect record is the system's formal enquiry artefact. This document uses both terms interchangeably.

This specification covers:

- The full lifecycle of a leasing enquiry from Lead creation to Customer ID generation
- Lead ingestion channels: manual, bulk upload, mobile, and API
- Lead qualification, assignment hierarchy, and interaction logging
- Identity validation: PAN (individuals), GSTIN (non-individuals)
- AML screening: UNSC name screening, beneficial ownership, politically exposed persons, passport CC database check
- Caution list screening: CCDB (RBI), UAPA, UN Sanctions, internal defaulter/fraud list, PAN caution list
- Bureau (CIBIL) credit report submission
- Hunter and Sherlock fraud detection
- Opportunity creation per line of business
- Pricing and Quote management: rack rate master, quote negotiation, authority matrix, quote lock
- Parallel KYC, AML, and CAM initiation from Prospect stage
- Enterprise Customer creation with full lineage preservation
- Re-KYC lifecycle management and AML gating rules

**In scope:** Individual lessees, co-lessees, commercial/non-individual entities, group leasing arrangements (e.g., corporate fleet lessees such as Infosys or Microsoft).

**Out of scope:** Downstream contract configuration, billing engine, accounting posting (covered in Contract Configuration FS).

---

## 2. Architecture and System Components

### 2.1 System Architecture

```
Mobile / Desktop / API / Bulk Upload
          │
          ▼
  Lead Management Module
          │
          ▼
  Enquiry (Prospect) Module ← Primary entry point for compliance processing
          │
          ├──► Identity Validation (PAN API / GSTIN API)
          │
          ├──► AML Screening Engine (Name Screening / BO / PEP / Re-KYC)
          │
          ├──► PS_PK_COM_VAL_CONTACT     ← Contact/applicant identity validator (KYC, PAN, TAN, dedup, NRI)
          │
          ├──► PS_PK_LN_CAUTION_CUST_DB  ← Caution/compliance screener (CCDB, UAPA, UN, PAN-caution)
          │
          ├──► Hunter / Sherlock API     ← Fraud detection (external HTTP)
          │
          ├──► CIBIL Bureau API          ← Credit report (external HTTP)
          │
          ├──► Opportunity Module        ← Asset interest and LoB tagging
          │
          └──► Pricing & Quote Module    ← Rack Rate master, Quote creation, Negotiation, Lock
```

### 2.2 Package Roles (Reused from Lending Platform)

| Package | Role |
|---|---|
| `PS_PK_LN_LOAN_ENQUIRY_GEN` | Master orchestrator for enquiry lifecycle; adapted for leasing contract type |
| `PS_PK_COM_VAL_CONTACT` | Contact/applicant identity validator; ~95 fields, KYC, PAN, TAN, Aadhaar, dedup, NRI, deceased handling |
| `PS_PK_LN_CAUTION_CUST_DB` | Caution database screener: CCDB, UAPA, UN Sanctions, internal, PAN caution |

New leasing-specific modules:

| Module | Role |
|---|---|
| Lead Management Module | Lead ingestion, assignment hierarchy, interaction logging, status classification, promotion to Prospect |
| AML Screening Engine | UNSC name screening, Beneficial Ownership, PEP checks, Re-KYC scheduling and gating |
| Opportunity Module | Opportunity creation per LoB, asset tagging, quote linkage |
| Pricing & Quote Module | Rack Rate master, Quote ID generation, negotiation band enforcement, authority matrix, quote lock |
| Enterprise Customer Module | Customer ID creation, lineage management, role assignment, downstream contract trigger |

---

## 3. End-to-End Leasing Origination Lifecycle

```
[LEAD CREATION]  ─────────────────────────────────────────────────────────────────
 P1. Base Lead Record (Temp Customer No. + LeadRefNo)
 P2. Bulk Upload / API Ingestion
 P3. Dedup / Existence Check (PAN/GST/Mobile/Email)
 P4. Lead Assignment Hierarchy (CPU → Branch Manager → Field Officer)
 P5. Interaction Logging & Next Action Scheduling
 P6. Lead Qualification (Cold / Warm / Hot)
 P7. Promote to Prospect / Close
          │
          ▼
[ENQUIRY (PROSPECT) CREATION] ────────────────────────────────────────────────────
 P3'. PAN/GSTIN Validation + Legal Name/Address Enrichment
 AML1. Name Screening (UNSC, UAPA, PEP, Beneficial Ownership)
 AML2. PII Capture
 AML3. Beneficial Owner Codes
 AML4. AML Risk Profiling & Categorisation
 Caution List Screening (CCDB, Internal, PAN-Caution, Passport-CC)
 KYC Validation (PS_PK_COM_VAL_CONTACT)
 CIBIL Bureau Request
 Hunter / Sherlock Fraud Detection
 Enquiry (Prospect) ID generated
          │
          ▼
[OPPORTUNITY MANAGEMENT] ──────────────────────────────────────────────────────────
 P5'. Opportunity Creation per LoB (Asset Category + Asset Class mandatory)
 Multi-LoB Opportunities supported (Leasing / Lending / Deposits / Others)
          │
          ▼
[PRICING & QUOTE MANAGEMENT] ──────────────────────────────────────────────────────
 P1'. Rack Rate Quote Master lookup (R#########)
 P2'. Quote Creation (Q#########), Negotiation, Authority Matrix, Lock
 P3'. Corporate Parameter Change / Bulk Revision (if applicable)
          │
          ▼
[APPLICATION ORIGINATION] ─────────────────────────────────────────────────────────
 KYC, AML (AML5–AML7), CKYC in parallel
 CAM (Credit Appraisal Memo) initiation
 AML Re-KYC gating (AML6, AML8)
          │
          ▼
[ENTERPRISE CUSTOMER CREATION] ────────────────────────────────────────────────────
 KYC = Complete AND CAM = Approved → Enterprise Customer ID created
 AML1 for Co-Lessees; AML3 (BO); AML4 (Risk Setting) before role tagging
 Full Lineage: Lead → Prospect → Opportunity → Quote → Application → Customer
 Role assignment: Lessee / Co-Lessee / Dealer / DMA / DSA / Depositor
          │
          ▼
[CONTRACT CONFIGURATION] ─────────────────────────────────────────────────────────
 (Out of scope — covered in Contract Configuration FS)
```

---

## 4. Stage 1 — Lead Generation

### 4.1 Purpose

Lead Generation captures the prospective lessee's intent and registers a base record before formal identity validation. The lead funnel supports individual and commercial (group) leasing, multi-source ingestion, and a four-level assignment hierarchy.

### 4.2 Lead Types

| Lead Type | Description | Additional Fields |
|---|---|---|
| Individual | A natural person seeking a lease | Name, Mobile, Email, PAN (optional at base) |
| Commercial | An entity or group (e.g., corporate fleet leasing for Microsoft/Infosys employees) | Company (Known As), Contact Person, GSTIN (optional at base) |

### 4.3 P1 — Base Lead Record Creation (Manual)

**Entry Channels:** Mobile app, Desktop form, Call Centre Associate, Field Officer, Central Processing Team (CPU).

**Prerequisites:** Creator has access; at least one mandatory identifier available: Name + one of Mobile / Email / PAN / GSTIN / Address.

**Process:**
1. Capture Lead Type (Individual / Commercial).
2. For Commercial leads, capture Company Known-As name and Contact Person.
3. Generate **Temp Customer No.** (format: `TMP-YYYY-NNNNNN`) and **LeadRefNo** (format: `LOB-YYYYMM-NNNNNN`). Both are unique and system-generated.
4. Capture Source Category (Internal / External / Campaign / Dealer) and Source Name (e.g., `Diwali-CAR-LEASE-2025`).
5. Optionally capture PAN (Individual) or GSTIN (Commercial) and store geotag (latitude/longitude) when created from mobile.
6. Multiple Email IDs must be supported at lead capture.
7. Default assignment: CPU unless self-assigned is selected by creator.

**Validations:**
- At least one of Mobile / Email / PAN / GSTIN / Address is mandatory. Leads without mandatory fields → Exception Queue with reason code.
- Lead Type mandatory before save.
- Source Category and Source Name mandatory.

### 4.4 P2 — Lead Ingestion (Bulk Upload / API)

**Channels:** Excel template upload, External/Internal API.

**Validations:**
- Each row/payload validated for mandatory identifier set: Name + one identifier.
- Invalid records → Exception Queue with reason codes; no unassigned leads after successful ingestion.
- API authentication must be configured; schema validation enforced.
- Upload template approval required before processing.
- LeadRefNo generated in defined format per record.

### 4.5 P3 — Identity Capture and Existence Check / De-duplication

**Dedup Identifiers:** Mobile, Email, PAN, GSTIN.

**DedupLabel values:**

| Label | Meaning |
|---|---|
| `PossibleExisting` | Identity matches a known UCIC or customer code |
| `Unknown` | No strong match found |
| `Conflict` | PAN/GSTIN maps to multiple UCICs — requires CPU review |
| `New` | No match; first-time prospect |

**Rules:**
- DedupLabel is mandatory before lead is allowed to progress to Prospect.
- Conflicts must be resolved in Exception/Review queue with audit trail.
- If the lead's identity matches an existing customer in the system, the **current role** of that contact must be displayed to the Field Officer or CPU (e.g., GL Supplier, Service Provider, Depositor, Lessee, Guarantor). This prevents inadvertent duplicate customer creation.
- UCIC and all mapped customer codes must be displayed when a match is found.
- PAN/GSTIN becomes mandatory at Prospect promotion (optional at Base stage).

### 4.6 P4 — Lead Assignment and Re-assignment (Four-Level Hierarchy)

**Hierarchy:**

```
CPU (Level 1) → Branch Manager (Level 2) → Field Officer / Associate (Level 3) → (Sub-level, if applicable) (Level 4)
```

**Rules:**
- Default owner: CPU for all created/uploaded leads.
- CPU assigns to Branch and optionally to a Call Centre Associate for initial follow-up.
- CPU assigns to Branch Manager; Branch Manager assigns to Field Officer.
- Re-assignment: Branch Manager can re-assign to another officer, send back to CPU, or escalate.
- Each assignment/re-assignment must capture remarks, reason code, and timestamp.
- No lead should remain unassigned at any time.
- SLA timers start on each assignment; breach triggers escalation (configurable matrix).
- FO absence rule: escalation triggered if FO is unavailable within SLA.

### 4.7 P5 — Interaction Logging and Next Action Scheduling

**Interaction Types:** Phone call, email, WhatsApp/message, meeting, video call.

**Fields per Interaction:**
- Timestamp (mandatory)
- Interaction type (mandatory)
- Outcome (free-text notes)
- Next follow-up: date, time, mode, contact person (mandatory for in-progress leads)

**Counters:** Attempt counters maintained per channel (calls / messages / emails) per lead; used for status classification rules.

**Visibility:** Role-based; interaction history is immutable once logged.

### 4.8 P6 — Lead Qualification and Status Classification

**Temperature Buckets:** Cold / Warm / Hot.

**Classification rules:**
- System suggests status based on attempt counters, interaction responses, and data completeness.
- User may override with a mandatory StatusReason.
- Degradation rules: Hot → Warm → Cold triggered by inactivity or negative outcomes.
- Manual overrides require `StatusSuggestedBy = 'Manual'` and `StatusReason`.
- All status changes are auditable.

### 4.9 P7 — Move to Prospect / Closure

**Prospect Promotion:**
- Lead must have PAN (Individual) or GSTIN (Commercial) — mandatory at promotion (addendum policy).
- DedupLabel must be resolved.
- UCIC mapping performed if applicable.
- Triggers Prospect record creation with Prospect ID generation.

**Closure:**
- Standardized Closure Reason Code mandatory.
- Interaction history immutable post-closure.
- Closed leads cannot be re-assigned.

### 4.10 P8 — Dashboards and To-Do Lists

**Role-based dashboards for:** CPU, Branch Manager, Call Centre, Field Officers, RVP/Regional/Branch Heads.

**KPIs:**
- Lead aging, first-contact TAT, attempts per lead, response rate, data completeness, conversion-to-Prospect rate, closure reasons.

**SLA reporting:**
- Time-bound first action, exception cure SLA, FO absence rule, escalation matrix. All breaches logged and reportable.

**Filters:** Branch, LoB, source, status, owner; drill-down capable.

---

## 5. Stage 2 — Enquiry (Prospect) Creation

### 5.1 Purpose

The Prospect stage is the formal entry point where identity is validated, compliance checks are performed, and the record becomes the basis for all downstream processing. The Prospect record corresponds to the Enquiry in lending terminology and generates the **Prospect ID** — the primary business key equivalent to `enquiry_number` in lending.

### 5.2 Entry Points

| Mode | Source | Trigger |
|---|---|---|
| Lead Promotion | Lead Management | Lead with PAN/GSTIN promoted to Prospect |
| Direct Prospect Creation | Branch / CPU (existing customer) | Policy permits starting at Prospect for known customers |
| Mobile Lead | Mobile App | Lead created via mobile promoted to Prospect |
| Re-submission | Branch | Re-submit CIBIL / AML for a Prospect without bureau ID |

### 5.3 Step-by-Step Process Flow (Lead-Promoted Prospect)

#### Step 1 — Receive Confirmed Lead

**Source:** Lead Management Module, mode `Lead Promotion`.

Validated inputs required at point of conversion:
- PAN (Individual) or GSTIN (Commercial) — mandatory
- Contact name
- Mobile / Email — at least one

The confirmed lead must carry a resolved DedupLabel. If not resolved, conversion is blocked.

#### Step 2 — Branch Validation

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_val_enquiry_branch` (adapted for leasing contract type)  
**Table:** `ps_tb_branch_defn`

Checks (same as lending):
- Branch exists for the company
- `business_open_date IS NOT NULL` and `business_close_date IS NULL`
- `account_open_date IS NOT NULL` and `account_close_date IS NULL`

**Error:** `LN480` — "Invalid Branch Code".

#### Step 3 — Marketing Employee / Associate Lookup

**Table:** `ps_tb_user_defn`

Fetches `user_id` for the assigned associate (`sf_mkt_emp_code`) where `user_status = 'A'`. For Vruddhi branches (component code `PS01076`), the user and department mapping is mandatory.

**Error:** `GL461` — "Invalid User".

#### Step 4 — PAN / GSTIN Validation and Legal Name/Address Enrichment

**For Individuals:**
- Validate PAN against Income Tax PAN API.
- Fetch legal name and registered address from API response.
- Store alias/brand name (known-as) alongside legal name.
- Capture validation response metadata: source, timestamp.
- Derive PAN where applicable (e.g., from GSTIN for commercial entities).

**For Non-Individuals (Commercial):**
- Validate GSTIN against GST Portal API.
- Fetch legal entity name and registered address.
- Store alias/brand name.

**PAN Format Validation** (`PS_PK_COM_VAL_CONTACT.ps_pr_pan_no_validation`):  
Format: `[A-Z]{5}[0-9]{4}[A-Z]{1}` — 10-character alphanumeric.

Constitution-specific 4th-character rule:
- Individual → `'P'`; Company → `'C'`; Partnership → `'F'`; HUF → `'H'`

**GSTIN Format Validation** (`PS_PK_COM_VAL_CONTACT.ps_pr_val_dealer_gstin`):  
15-character format: `[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}`  
First 2 digits = state code; characters 3–12 = embedded PAN.

**PAN De-duplication** (`ps_pr_pan_dedup_val_api_call` — PJT-1517):  
Calls API with PAN, customer name, DOB, constitution code, group ID. Blocks transaction if PAN belongs to a different contact with a different name.

**PAN Mandatory Validation** (`ps_pr_pan_mandate_val_api_call` — PJT-943):  
Validates PAN is provided for all constitution types mandated by regulation.

**Failure Handling:** Validation failures route to exception handling with reason codes and mandatory audit trail. Legal name and registered address must be populated from validated source; manual override requires reason and audit trail.

#### Step 5 — PAN Caution Gate (Pre-Insert Hard Block)

**Function:** `PS_PK_LN_CAUTION_CUST_DB.ps_fn_pan_status_chk(iv_v_pan)`

Called synchronously before any insert for every applicant with a PAN.
- Returns `'Allowed'` if PAN is not on the PAN-based caution list.
- Returns blocking reason string if PAN is on the caution list.

**Error:** `LN5337` — "PAN Blocked" — hard stop; transaction cannot proceed until resolved.

#### Step 6 — Passport CC Database Check (Hard Block)

**Trigger:** Applicant has a Passport.

**Logic:**
- Check whether the passport number matches any record in the external Central Caution (CC) Database.
- If match found → flag as "Undesirable Prospect" and **block further processing**.
- Additionally, check applicant name (excluding title: Initial + First Name + Middle Name + Last Name) against the CC database. If name matches:
  - Flag as "Undesirable Customer".
  - If DOB is also available, compare DOB (supporting YYYY / MM-YYYY / DD-MM-YYYY / date range formats). Matching DOB escalates to confirmed "Undesirable Customer".

This is a **hard block** — the prospect/enquiry cannot be saved if the passport or name is positively matched in the CC database.

#### Step 7 — AML Name Screening (AML1 — See Section 7)

Full AML screening is performed at this stage for all parties (Lessee, Co-Lessee, Guarantor, Beneficial Owners).

#### Step 8 — Enquiry Timeline Validation

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_val_enquiry_branch` (overload with timeline outputs)

Validated against configurable permissible-day windows:
- `fo_enq_permissable_days` — Field Officer back-dating window
- `br_enq_permissable_days` — Branch permissible window
- `br_apl_permissable_days` — Branch application window

If `enquiry_block_status = 'Y'`, enquiry creation is halted.

#### Step 9 — Asset and Contract Type Validation

**Table:** `ps_tb_ln_loan_enq_asset_ctrl`

For leasing, validates that the asset class and contract type combination (Finance Lease / Operating Lease) is permitted for the originating branch. If not permitted, blocks the transaction.

#### Step 10 — Prospect ID (Enquiry Number) Generation

**Function:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_fn_ctrl_num_gen`  
**Parameters:** `company_code`, `doc_type = 'ENQ'` (leasing-specific prefix to be configured), `branch_code`

Generates a sequential, branch-specific Prospect/Enquiry ID. Format configured in the control number configuration table. The generated ID is the primary business key (equivalent to `enquiry_number` in lending).

Format: `PR-YYYY-NNNNNN` (or as configured per leasing doc_type).

#### Step 11 — Insert Prospect (Enquiry) Header

**Table:** `ps_tb_ln_loan_enquiry_hdr` (adapted for leasing contract type) or new `ps_tb_ls_prospect_hdr`

Fields on first insert:

| Field | Value |
|---|---|
| `prospect_id` | Generated by `ps_fn_ctrl_num_gen` |
| `prospect_status` | `'PE'` (Pending/Draft) |
| `prospect_date` | `TRUNC(NVL(iv_d_enquiry_date, SYSDATE))` |
| `contract_type` | Finance Lease / Operating Lease |
| `asset_class_code` | Asset class being leased |
| `source_of_business` | Dealer / Direct / QR / API |
| `device_ind` | `'D'` Desktop / `'M'` Mobile |
| `enquiry_source` | `'B'` Branch / `'M'` Mobile / `'A'` API |
| `lead_ref_no` | Link to Lead record |
| `temp_customer_no` | Link to lead temp customer |
| `hunter_result` | NULL (set after Hunter call) |
| `cibil_status` | NULL (set after CIBIL call) |
| `aml_status` | NULL (set after AML screening) |
| `pan_validated_ind` | `'Y'`/`'N'` |
| `gstin_validated_ind` | `'Y'`/`'N'` |
| `org_branch_code` | Original branch (preserved on transfer) |
| `lob_type` | `'LEASING'` |

**Group Leasing fields** (for commercial / corporate fleet):
- `group_lob_ind` — `'Y'` for group leasing arrangements
- `preferred_customer_ind` — `'Y'` if linked to a preferred rate rack (e.g., Microsoft, Infosys)
- `company_code_lessee` — Corporate entity code

#### Step 12 — Insert Prospect Detail Row(s) (Applicant-Level)

**Table:** `ps_tb_ln_loan_enquiry_dtls` or `ps_tb_ls_prospect_dtls`

One row per applicant (Lessee, Co-Lessee, Guarantor). For each:
- `applicant_sl_no` — Sequential (1 = Main Lessee, 2+ = Co-Lessees/Guarantors)
- `contact_id`, `contact_code` — From contact master
- `applicant_role` — `'LS'` Lessee / `'CL'` Co-Lessee / `'GR'` Guarantor
- `cibil_req_no`, `cibil_ref_id` — NULL until bureau request
- `aml_case_id` — AML screening reference
- Address, KYC, telecom fields (same as lending detail)

#### Step 13 — Contact Validation (Per Applicant)

**Package:** `PS_PK_COM_VAL_CONTACT`

Full contact validation covering ~95 identity and demographic fields. Identical to lending process. See Section 12 for field-level rules.

#### Step 14 — KYC Document Validation

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_val_kyc_dtls`

Validates KYC identifiers (PAN, Driving Licence, Passport, Voter ID, Aadhaar) against contact master. Overload 2 (PJT-1517) additionally performs PAN-level de-duplication at KYC level.

#### Step 15 — CIBIL Bureau Request

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_ln_iud_bureau_enq_dtls`

For each applicant where `cibil_req_no IS NULL`:
- Constructs bureau request with full name, address, DOB, all KYC documents, mobile, telephone.
- Inserts into `ps_tb_ln_bureau_enq_dtls` and calls CIBIL API.
- On success: updates `cibil_req_no`, `cibil_ref_id` on detail; `cibil_status` on header.
- Re-submission (mode `R`): only applicants with `cibil_req_no IS NULL` are re-submitted.

CIBIL failure is **non-blocking** — Prospect is saved with `cibil_status = NULL`; requires manual resolution.

#### Step 16 — Caution List Screening

**Package:** `PS_PK_LN_CAUTION_CUST_DB.ps_pr_val_caution_list`

Called per applicant. Covers three domains:
1. External caution (CCDB/RBI + UN Sanctions)
2. Internal caution (company defaulter/fraud list, PAN, Aadhaar match)
3. Other caution (UAPA)

Caution hit is **non-blocking by default** (enquiry is flagged and emailed to branch manager). Only the PAN caution gate (Step 5) and Passport CC gate (Step 6) are hard blocks.

See Section 13 for full screening rules.

#### Step 17 — Hunter / Sherlock Fraud Detection

**Packages:** `ps_pr_req_hunter_xml`, `ps_pr_hunter_sherlock_xml`, `ps_pr_upd_hunter_result`

Applicability check: `ps_pr_enq_hunter_appl` — based on mobile number and branch rules.

If applicable:
1. Build Hunter XML / Sherlock JSON payload: name, DOB, PAN, address, mobile, KYC docs, applicant role.
2. Post to Hunter API (external HTTP).
3. Update `hunter_result` and `hunter_verification_result` on header.
4. Build Sherlock payload and post to internal Sherlock engine.
5. Update `sherlock_result` and `sherlock_verification_result` on header.

Result values: `P` (Pass) / `F` (Fail) / `M` (Manual review pending).

Hunter FAIL → sets `hunter_verification_result = 'F'`; requires manual decision by Branch Manager. Non-blocking for Prospect save.

Note: As of patch 12-Mar-2026 (ticket 938043), Hunter validation is also applied at contract confirmation stage.

#### Step 18 — SMS to Customer

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_enq_sms_to_customer`

If `sms_ind = 'Y'`:
- SMS sent to main lessee's registered mobile.
- Content includes Prospect ID and branch details.
- Optionally includes URL for document upload (CL7006 feature).
- Consent flow (CL1870): customer reply (OTP-based) tracked via `ps_pr_enq_decision_otp`.

#### Step 19 — STP (Straight Through Processing) Check

For existing lessees in good standing, STP eligibility is evaluated:
1. `ps_pr_enq_stp_process` builds JSON payload and returns service URL.
2. External STP engine evaluates.
3. `ps_pr_enq_stp_response` processes response and records STP decision.

If STP approved (`ov_v_service_call_ind = 'Y'`), Prospect transitions toward Application status.

#### Step 20 — Decision Marking

**Package:** `PS_PK_LN_LOAN_ENQUIRY_GEN.ps_pr_enq_decision_marking`

Evaluates automated credit decision eligibility based on branch, constitution type, asset class, and contract type.

Returns `ov_v_enq_dec_ind`:
- `'A'` — Auto-approved
- `'R'` — Auto-rejected
- `'M'` — Manual review required

### 5.4 Prospect Assignment and Re-assignment

After Prospect creation, assignment follows the same hierarchy as Lead (CPU → Branch Manager → Associate). Rules:
- No Prospect shall remain unassigned.
- Branch change after assignment allowed only through exception workflow with approval.
- Remarks mandatory for every assignment/re-assignment action.
- All assignment history is auditable.

### 5.5 Meeting and Interaction Management at Prospect Stage

**Fields per Meeting/Interaction:**
- Meeting ID (system-generated, e.g., `MTG-YYYY-NNNNN`)
- Date & time (mandatory)
- Attendees and designations
- Notes/remarks (mandatory)
- Outcome
- Next follow-up scheduling (optional reminders)

Multiple meetings per Prospect supported. History is role-based and auditable.

---

## 6. Identity Validation — PAN and GSTIN

### 6.1 PAN Validation (Individuals)

**Procedure:** `PS_PK_COM_VAL_CONTACT.ps_pr_pan_no_validation`

**Format check:** `[A-Z]{5}[0-9]{4}[A-Z]{1}` — 10-character alphanumeric.

**Constitution-specific rules:**

| Constitution Type | 4th Character |
|---|---|
| Individual | `P` |
| Company | `C` |
| Partnership/LLP | `F` |
| Hindu Undivided Family | `H` |
| Association of Persons | `A` |

**PAN API integration:**
- Validates PAN against Income Tax Department API.
- Returns legal name and address.
- Response metadata (source, timestamp) stored.

**PAN de-duplication** (`ps_pr_pan_dedup_val_api_call` — PJT-1517):
- Parameters: PAN, customer name, DOB, constitution code, group ID, product (`LOS`), process (`ENQUIRY`), reference no (Prospect ID), asset class, customer role.
- For sole proprietorships: proprietor name and DOB additionally passed.
- Blocks if PAN belongs to a different contact with a different name.

**PAN mandatory validation** (`ps_pr_pan_mandate_val_api_call` — PJT-943):
- Enforces PAN provision for all regulated constitution types.

### 6.2 GSTIN Validation (Non-Individuals / Commercial)

**Procedure:** `PS_PK_COM_VAL_CONTACT.ps_pr_val_dealer_gstin`

**Format:** 15-character GST identification number: `[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}`

**GST Portal API integration:**
- Validates GSTIN.
- Returns legal entity name and registered address.
- First 2 digits = state code; characters 3–12 = embedded PAN (cross-validated).

**GSTIN failure:** Routes to exception handling with reason code; Prospect cannot be created without successful GSTIN validation for commercial entities.

### 6.3 TAN Validation

**Procedures:** `PS_PK_COM_VAL_CONTACT.ps_pr_tan_validation`, `ps_pr_tan_no_val`, `ps_pr_val_cont_tan`

**Format:** `[A-Z]{4}[0-9]{5}[A-Z]{1}` — 10-character alphanumeric.

Supports up to 5 TAN numbers per customer (PJT-1135).

---

## 7. AML Checks

### 7.1 Overview

The Leasing AML framework comprises 10 named processes (AML1–AML10) defined in the Prospect Creation RS. These cover initial onboarding screening, beneficial ownership, risk profiling, Re-KYC lifecycle management, and regulatory reporting. The AML engine must integrate with external sanctions databases and internal watchlists.

**Parties screened:** Lessee, Co-Lessee, Beneficial Owners (BOs), DMA/DSA, and any other parties linked to the leasing transaction.

### 7.2 AML1 — Name Screening (Prospect and Application Stages)

#### 7.2.1 Trigger Points

- During Prospect validation (Step 7 of Section 5.3).
- On any change to name/identifiers during Prospect or Customer creation — re-screen the party.
- On new additions to sanctions lists — batch re-screening of all existing customers; exceptions to AML Team immediately.
- During Credit/CAM stage (Application Origination) for Lessees.
- At Customer Code creation for Co-Lessees.

#### 7.2.2 Sanctions Lists Screened

| List | UN Resolution / Authority |
|---|---|
| Al-Qaeda | 1267/1989 |
| Taliban | 1988 |
| Iran | 2231 |
| Democratic Republic of Congo (DRC) | 1533 |
| Democratic People's Republic of Korea (DPRK) | 1718 |
| Iraq | 1518 |
| Sudan | 1591 |
| Al-Shabaab | 1844 |
| Libya | 1970 |
| Guinea-Bissau | 2048 |
| Central African Republic | 2127 |
| Yemen | 2140 |
| South Sudan Republic | 2206 |
| Haiti | 2653 |
| Mali | 2374 |
| Unlawful Activities Prevention Act (UAPA) | MHA / Ministry of Home Affairs |
| Politically Exposed Persons (PEP) | Internal/external PEP registry |
| Beneficial Ownership | RBI mandate |

#### 7.2.3 Name Matching Logic

The system must screen against all three pre-computed name variants stored in the AML/caution master:
- `Good_Quality_Name` — Cleaned, de-spaced version of the sanctioned name
- `Low_Quality_Name` — Phonetic/variant of the sanctioned name
- `Name_Original_Script` — Full name as it appears in the original sanctions document (First + Second + Third + Fourth name components)

**Full Name Matching:** Concatenated name from all components (`<First_name> + <Second_name> + <Third_name> + <Fourth_name>`) compared against the customer's full name across Good Quality, Low Quality, and Original Script variants.

**Partial Name Matching:** If any single name component of the customer (First / Middle / Last) partially matches Good Quality, Low Quality, or Original Script of a sanctioned name, the system must flag the match.

**Example:**
> Customer name: `AMIR KHAN`  
> Sanction list entry: `AMIR KHAN MOTAQI`  
> Rule: "AMIR KHAN" is a partial match of "AMIR KHAN MOTAQI" → flag and trigger auto-mail, regardless of whether DOB is available.

**Cross-name component matching example:**
> Customer: `MOHAMMAD SALIM`  
> Sanction entries: `MOHAMMAD ALI AL HABBO` and `SALIM MUSTAFA`  
> Rule: "Mohammad" partially matches "Mohammad Ali Al Habbo" AND "Salim" partially matches "Salim Mustafa" → both sanction names must be reported.

#### 7.2.4 Applicant Role Coverage

All applicant roles must be screened individually:
- Lessee (Main Applicant)
- Co-Lessee / Co-Borrower
- Additional Applicant 1
- Additional Applicant 2
- Guarantor

Auto-mail is triggered if **any** role's name partially or fully matches any sanctions list, regardless of DOB availability.

#### 7.2.5 DOB Matching

DOB matching supports all of the following formats as present in the sanctions list:
- Year only: `YYYY`
- Month + Year: `MM-YYYY`
- Full date: `DD-MM-YYYY`
- Date range: `Start_DOB` to `End_DOB`

The system must trigger alerts even when DOB is **not available** in the sanctions list. DOB match confirms the hit further but absence of DOB does not prevent flagging.

#### 7.2.6 AML Match Outcomes

| Outcome | System Action |
|---|---|
| No match | Screening passes; AML status = `Clear` |
| Partial match (any role/name component) | Flag; trigger auto-mail to AML Team |
| Full match | Flag as "True Match"; block Prospect/Application pending AML clearance |
| Confirmed "True Match" (post investigation) | Prospect/application blocked; whitelist decision required from AML Head |

#### 7.2.7 Whitelisting

Post-investigation, whitelisting is recorded via `ps_pr_ln_upd_ccdb_whitelist_ind` with:
- Documented justification
- Authorised approver (AML Head for True Match)
- Audit trail and email notification to stakeholders

### 7.3 AML2 — PII (Personally Identifiable Information) Capture

Capture granular PII for all Leasing parties (Lessee, Co-Lessee, DMA, DSA) in a dedicated tab within the enterprise customer setup screen:
- Occupation
- Industry sector
- Constitution type
- Employment type (salaried / self-employed / professional)
- Source of funds (declaration)
- Country of residence / nationality

PII fields persist to support AML screening context and risk profiling. Mandatory/optional field rules align with enterprise governance; any override requires reason and audit trail.

### 7.4 AML3 — Beneficial Owner (BO) Capture

**Trigger:** Non-individual prospect identified (Commercial/Company/Partnership/Trust).

**Process:**
- Capture BO details at Prospect update and Customer creation.
- Create a dedicated customer code for each Beneficial Owner, equivalent in status to Lessees/Borrowers.
- BOs are included in AML name screening (AML1) and risk profiling (AML4).

**Data per BO:** Name, PAN, DOB, address, nationality, percentage of ownership, nature of control.

**Note:** Even where Lending/Deposits do not maintain separate BO codes, Leasing maintains BO as separate customer codes for AML parity per RBI requirements.

### 7.5 AML4 — AML Risk Profiling and Categorisation

**Trigger:** At customer code creation / role assignment.

**Risk determination logic:**
- Assign profile-based AML risk rating based on risk matrix.
- Final risk = `MAX(profile_risk, exposure_risk)` evaluated at PAN level for multiple customer codes.
- NRIs, Online customers, and PEP-NRI customers → default to High Risk (excluded from matrix-based determination).
- Enterprise risk matrix/programme maintained as-is; no change to existing logic.

**Risk Categories:** High / Medium / Low.

### 7.6 AML5 — Re-KYC Maintenance and Updates

**Trigger:** Re-KYC due date reached OR customer submits fresh KYC during new-business processing.

**Process:**
- Capture new KYC documents and post a new entry in the Risk Categorisation tab.
- Derive next Re-KYC due date based on risk category.
- During contract creation: if customer opts to update KYC, prompt `Update KYC (Y/N)`.
- If Yes: post entry to Risk Categorisation; derive new due date.

**Frequency:** Per enterprise standards (consistent with Lending/Deposits).

### 7.7 AML6 — Re-KYC Gate (Payment and Contract Blocks)

**Trigger:** Re-KYC status = Pending for any party (Lessee, Co-Lessee, Dealer, DMA/DSA, Broker).

**Rule:** Block payments and new contract creation until Re-KYC is completed for the relevant party.

**Override:** No override without AML Team approval; exceptions must be audited with reason codes.

### 7.8 AML7 — Re-KYC Reminders

**Channels:** WhatsApp, Email, Registered Mobile Number (RMN) — per RBI regulatory direction.

Existing enterprise reminder logic and schedules are used; delivery and response status tracked; escalations per SLA.

### 7.9 AML8 — Beneficial Ownership Declaration (BOD) Gate

**Trigger:** BOD status = Pending for any Lessee or linked BO.

**Rule:** Block payments and contract creation until BOD = Complete.

**Override:** No downstream processing until BOD = Complete; exceptions require AML Head approval.

### 7.10 AML9 — Auto Risk Categorisation (Half-Yearly Batch)

**Schedule:** 30 June and 31 December each year.

**Process:** Post enterprise-level risk categorisation entries for all Lessees, Co-Lessees, and BOs based on exposure as on the cut-off date (per RBI Master Direction on KYC). Batch completeness validated; audit logs produced; exposure cut-offs reconciled.

### 7.11 AML10 — AML Reporting (Quarterly/Enterprise)

**Scope:** Enterprise-level datasets including Leasing submitted to RBI quarterly.

**Includes:** Live/existing customer lists, debit/credit transactions, constitution/risk-based data, Re-KYC pending, closed accounts, BO collection/pending, Risk Variation.

**Delivery:** Power BI datasets; AML MIS; Regulatory reporting interface (enterprise). RBAC enforced; lineage and period coverage validated.

---

## 8. Stage 3 — Opportunity Management

### 8.1 Purpose

An Opportunity represents a specific asset interest of a Prospect within a Line of Business (LoB). Multiple Opportunities per Prospect are supported, one per LoB.

### 8.2 P5 — Opportunity Creation

**Prerequisite:** Prospect validated, AML = Clear (or under review with no hard block), and active.

**Process:**
1. Associate identifies LoB interest (Leasing / Lending / Deposits / Others).
2. Create Opportunity ID (system-generated, e.g., `OPP-YYYY-NNNNN`).
3. Capture mandatory fields:
   - Asset Category (mandatory)
   - Asset Class (mandatory)
4. Capture optional fields:
   - Make, Model/Variant/Type
   - Potential Business — Volume (units) and Value (amount)
   - Discussion context / notes
4. If more than one LoB discussion occurs, create separate Opportunity per LoB.
5. Tag each Opportunity with contact/discussion context.

**For Group/Preferred Leasing:**
- Rack Rate lookup must check whether the Prospect is a preferred company (Microsoft, Infosys, etc.) and apply preferred rates if `preferred_customer_ind = 'Y'`.

**Validations:**
- Asset Category and Asset Class are mandatory.
- Multiple Opportunities per Prospect allowed; each must have a LoB tag.
- Duplicate Opportunity creation rules (same asset + same LoB) are configurable.
- LoB tag mandatory.

### 8.3 Opportunity Status Lifecycle

| Status | Meaning |
|---|---|
| `Open` | Opportunity created; no quote yet |
| `Quoted` | Rack rate or customized quote generated |
| `Negotiation` | Quote under negotiation |
| `QuoteLocked` | Final quote confirmed and locked |
| `Converted` | Application initiated; Opportunity linked to contract pipeline |
| `Closed` | Opportunity closed without conversion |

---

## 9. Stage 4 — Pricing and Quote Management

### 9.1 Purpose

The Pricing and Quote module governs the creation of lease pricing (Rack Rate master), lessee-specific quote generation, negotiation controls, and quote finalization. A finalized and locked quote is the pricing baseline for Contract Configuration.

### 9.2 P1 — Rack Rate Quote Master Creation and Maintenance

#### 9.2.1 Process

1. Pricing Analyst (Maker) creates Rack Rate record with code `R#########`.
2. Mandatory parameters captured:

| Parameter | Description |
|---|---|
| Asset Class / Category | Asset type for which rate applies |
| GST Rate | GST percentage (e.g., 18%) |
| Depreciation Rate | Depreciation % or IT block link |
| Lease Type | Finance Lease / Operating Lease |
| Tenure | Lease period in months |
| Residual Value Type | On Asset Cost / Flat / On WDV / Not Applicable |
| Residual Value % or Amount | Residual value at lease end |
| Cost of Funds (CoF) / Base Rate | Funding cost (e.g., 8.5% / 7.75%) |
| Corporate Tax | Corporate tax % used in NPV computation (e.g., 25%) |
| ATDR | After Tax Discount Rate (e.g., 10%) |
| LMF Type and % | Lease Management Fee |
| Deposit % | Lease security deposit |
| Supplier Credit Period | Days |
| BDF | Business Development Fee |
| Subvention | Subvention inputs |

3. Set Effective From and Effective To dates and version number.
4. Submit for Pricing Head (Checker) approval.
5. On approval: activate version; mark prior versions Inactive.
6. Preferred Rack Rates: Rates for preferred companies (e.g., Microsoft/Infosys) can be set up via the same Rack Rate master with `preferred_customer_ind` linkage.

#### 9.2.2 Rack Rate Effective Dating Rules

| Condition | Outcome |
|---|---|
| Effective window overlaps existing Active version for same R### | Hard stop — Overlap not allowed |
| No overlap but `approval_status ≠ Approved` | Block activation |
| No overlap and `approval_status = Approved` | Activate; available for quote consumption |

At most one Active version per Rack Rate Code for a given effective date window. Current quotes are made Inactive automatically when a new version is activated; manual inactivation also supported.

#### 9.2.3 Rack Rate Lifecycle

`Draft → PendingApproval → Approved → Active → Inactive`

### 9.3 P2 — Lessee Quote Creation, Negotiation and Finalization

#### 9.3.1 Process

1. Associate selects an Active Rack Rate Code (R#########).
2. Captures lessee/Prospect identifier and quote context:
   - Asset category/class, Lease Type, Period Type, Rental Frequency, Payment Type.
3. Performs limit/eligibility check against lessee offer/limit (if applicable).
4. Allows negotiation adjustments within the configured band.
5. Captures negotiation reason and history.
6. Routes for approval if variance exceeds band (see authority matrix).
7. On approval: assigns Quote/Price ID (`Q#########`), sets validity window, locks fields.
8. Links finalized quote to Offer/Contract.
9. Post-lock edits require re-opening via approval workflow; old locked version is immutable.

**Quote/Price ID format:** `Q#########` (unique, system-generated).

#### 9.3.2 Authority Matrix — Negotiation and Overrides

| Change Area | Metric | Auto-Approve | Manager Approval | Pricing Head | Finance/Policy Committee |
|---|---|---|---|---|---|
| Rental / PMPL | Variance % vs rack rental | ≤ 2% | > 2% and ≤ 5% | > 5% and ≤ 10% | > 10% |
| Discount % | Variance vs rack discount | ≤ 1% | > 1% and ≤ 3% | > 3% and ≤ 5% | > 5% |
| Residual Value | Δ RV% vs rack | Not allowed (default) | ≤ 1% (if enabled) | > 1% and ≤ 2% | > 2% |
| LMF | Δ LMF% vs rack | ≤ 0.10% | > 0.10% and ≤ 0.25% | > 0.25% and ≤ 0.50% | > 0.50% |
| Deposit | Δ Deposit% vs rack | ≤ 1% | > 1% and ≤ 2% | > 2% and ≤ 3% | > 3% |
| Tenure | Δ months vs rack slab | N/A | ≤ 3 months (if enabled) | > 3 months | Per risk policy |
| Corporate Tax/CoF/ATDR changes | Master changes | Not allowed in quote | Not allowed | Not allowed | Via rack-rate revision only |
| Quote unlock after lock | Unlock request | N/A | Allowed with remarks | Mandatory | If exposure changes |

When multiple components are negotiated, the **highest approval tier** applies.

Variance% = `(ProposedRental − RackRental) / RackRental × 100`

#### 9.3.3 Quote Validity and Contract Linkage

| Condition | Allow Link to Offer/Contract? |
|---|---|
| `QuoteStatus=Locked` AND `CurrentDate ≤ ValidTo` | Yes |
| `QuoteStatus ≠ Locked` | No — complete approvals and lock first |
| `CurrentDate > ValidTo` (Expired) | No — revalidation required (new version or extend with approval) |
| Quote unlocked after lock | Only new version (old version immutable) |

Validity window is configurable by product/segment (e.g., 30 / 60 / 90 days).

#### 9.3.4 Quote Frequency and Payment Type

| Frequency | Code | Allowed Payment Types |
|---|---|---|
| Monthly | `M` | AD, AR, MAD, MAR, SAD, SAR |
| Bi-monthly | `B` | AD, AR |
| Quarterly | `Q` | AD, AR |
| Half-yearly | `H` | AD, AR |
| Yearly | `Y` | AD, AR |

Quarter Month Indicator rules determine billing months for B/Q/H/Y frequencies.

**Rental date rules:**
- First rental date must comply with configured policy offset from agreement date.
- Last installment date must not exceed agreement end date.

#### 9.3.5 Quote Types Reference

| Type Code | Description |
|---|---|
| `B` | Base Quote |
| `D` | Derived Quote |
| `S` | Sub Quote |
| `L` | LD Quote (Lease Deposit) |
| `R` | Revision Quote |

#### 9.3.6 Quote Lifecycle

`Draft → Negotiation → PendingApproval (if deviation) → Approved → Locked → Expired / Cancelled`

### 9.4 P3 — Corporate Parameter Change and Bulk Rack Rate Revision

**Trigger:** Change in corporate parameters (Corporate Tax, Cost of Funds, ATDR).

**Process:**
1. Identify impacted Rack Rate Codes.
2. Generate revised versions (`R######### vN+1`) with updated parameter values and effective dating.
3. Submit batch for Maker-Checker approval with evidence and impact summary.
4. On approval: activate new versions; inactivate obsolete versions per policy.
5. Execute adoption policy:
   - `AdoptionFlag = TRUE` → re-map open draft/negotiation quotes to new rack version and recompute.
   - `AdoptionFlag = FALSE` → existing open quotes are not changed; only fresh quotes use new version.
   - Locked quotes → do not auto-update; require unlock → new version → approvals.
6. Notify stakeholders; generate governance logs and exception lists with Batch Run ID.

**Adoption Policy Table:**

| Record Type | Adoption Flag | Action |
|---|---|---|
| New quote | N/A | Use latest Active rack rate as-of quote date |
| Existing Draft/Negotiation quote | TRUE | Re-map to new rack version; recompute |
| Existing Draft/Negotiation quote | FALSE | Do not change; log skipped with reason |
| Locked quote linked to Offer/Contract | Any | Do not auto-update; unlock → new version → approvals |
| Existing contract | Policy-controlled | Via revision module (not pricing) |

### 9.5 P4 — Governance, Controls and Audit

- RBAC: restrict create/edit/approve by role.
- Maker-Checker: approver cannot be the same user as maker.
- Evidence attachments: pricing worksheet, deviation justification stored in DMS.
- Audit trail: field-level changes with before/after values; append-only.
- Exception queue: validation failures with SLA tracking.
- All approvals require remarks and timestamp.

---

## 10. Stage 5 — Application Origination

### 10.1 Purpose

Application Origination is triggered from the Prospect stage, initiating parallel KYC, AML, CKYC, and CAM (Credit Appraisal Memo) workflows. The Prospect record displays the consolidated status of all parallel streams.

### 10.2 Process

**Prerequisite:** Prospect exists, at least one Opportunity and/or Quote exists, user has origination permission.

1. Associate initiates application processing from Prospect.
2. Uploads documents with form-fill automation where available.
3. System initiates in parallel:
   - **AML Screening** (AML1 for Lessees; AML5–AML7 where Re-KYC is due)
   - **CKYC** (Central KYC Registry) verification
   - **KYC** document validation
   - **CAM** (Credit Appraisal Memo) workflow
4. Application ID generated (`APL-YYYY-NNNNN`).
5. Prospect status updated to `InAppraisal`.
6. For existing customers within sanction validity period: only limit checking may be required (policy-controlled).

**Prospect-level status tracking:**

| Stream | Statuses |
|---|---|
| AML | Not Started / In Screening / Clear / Flagged / Blocked |
| KYC | Not Started / In Progress / Complete / Rejected |
| CKYC | Not Started / Submitted / Verified |
| CAM | Not Started / Initiated / In Review / Approved / Rejected |

### 10.3 Document Checklist Enforcement

Stage-wise document checklists enforced by system. Required documents per constitution type per stage:

**Individuals:**
- PAN, Aadhaar, Voter ID / Passport / Driving Licence (any one)
- Latest salary certificate + IT returns (employed) or IT returns for 3 years (professionals)
- Bank statement

**Companies:**
- Certificate of Incorporation, MOA/AOA
- GSTIN Certificate, PAN
- Proof of address
- Balance Sheet and P&L for 3 years

**Partnership Firms:**
- Registration certificate (if registered), Partnership deed
- Power of Attorney
- ID proof of partners + proof of address
- GSTIN Certificate, Balance Sheet and P&L for 3 years

**Trusts and Foundations:**
- Registration certificate, Trust deed
- Power of Attorney
- ID of trustees, settlers, beneficiaries, founders
- Resolution of managing body, Proof of address, Proforma Invoice

### 10.4 AML Re-KYC Gating at Application Stage

- AML6: If Re-KYC is pending for any Lessee/Co-Lessee/Dealer/DMA, block application progression.
- AML8: If BOD is pending for any Lessee or linked BO, block application progression.
- No override without AML Head approval.

---

## 11. Stage 6 — Customer Creation and Lineage

### 11.1 Prerequisites for Customer Creation

Both of the following gates must be passed:
- **KYC Status = Complete** (scan copies of full KYC docs must be present in DMS at customer creation; physical documents required before disbursement)
- **CAM Status = Approved**
- Final Quote locked (if applicable)

### 11.2 Process

1. System creates **Enterprise Customer ID** upon gate passage.
2. AML1 performed for Co-Lessees at customer ID creation.
3. AML3 (BO codes) and AML4 (risk categorisation) completed before role tagging/onboarding.
4. Assign role(s) post customer creation: Lessee / Co-Lessee / Dealer / Vendor / Depositor / DMA / DSA.
5. Trigger downstream agreement/contract processes.

### 11.3 Lineage Preservation

Full lineage is mandatory and immutable once customer is created:

```
Lead (LeadRefNo / Temp Customer No.)
  └─► Prospect (Prospect ID)
        └─► Opportunity (Opportunity ID per LoB)
              └─► Quote (Quote/Price ID)
                    └─► Application (Application ID)
                          └─► Enterprise Customer ID
                                └─► Contract (Contract Configuration)
```

All cross-references are stored in a lineage/cross-reference service with immutable links.

### 11.4 Role Assignment

Checklist completion required per policy before each role is assigned. Role assignment triggers downstream agreement/contract processes specific to that role.

---

## 12. Contact and Applicant Validation Rules

All contact validation is performed by `PS_PK_COM_VAL_CONTACT` — same rules as lending, applied to leasing parties (Lessee / Co-Lessee / Guarantor / Beneficial Owner).

### 12.1 Mandatory Field Validation

**Procedure:** `ps_pr_attr_mandatory_val`

Based on:
- `constitution_type` (`'I'` Individual, `'C'` Company, `'P'` Partnership, etc.)
- `residential_type` (`'R'` Resident, `'N'` NRI)
- `subgl_type` (Lease vs Loan sub-ledger)

Minimum mandatory fields for Individual/Resident:
- `contact_name1` (First/Initials), `contact_name3` (Surname)
- `contact_dob`, `gender`
- At least one address record
- At least one telecom record
- At least one KYC document

### 12.2 Aadhaar Validation

**Procedure:** `ps_pr_aadhaar_no_validation`
- Must be 12 numeric digits
- First digit cannot be `0` or `1`

### 12.3 Passport Validation

**Procedure:** `ps_pr_passport_no_validation`
- Format: `[A-Z]{1}[0-9]{7}` — 8 characters; normalised to uppercase

### 12.4 Voter ID Validation

**Procedure:** `ps_pr_voter_id_validation`
- Format: `[A-Z]{3}[0-9]{7}` — 10 characters; normalised

### 12.5 Mobile Number Validation

**Procedure:** `ps_pr_val_mobile_no`
- Must be exactly 10 digits
- Cannot start with `0` or `1`
- Cannot be all-same-digit (e.g., 9999999999)
- Cannot be a known test/dummy number
- Invalid mobile number errors are logged to employee audit via `ps_pr_ins_emp_mobile_err_log` (INC-462292)

### 12.6 Email Validation

**Procedure:** `ps_pr_val_user_email` (CL1836)
- Standard email regex validation
- Mandatory for specific roles where email is required

### 12.7 De-duplication

**Procedure:** `ps_pr_de_dup_val` (2 overloads)

- Overload 1 — Address/telecom-based dedup: name + DOB + address + mobile combination
- Overload 2 — Document-based dedup: PAN, Voter ID, Passport, Driving Licence, Aadhaar

**CKYC de-duplication** (`ps_pr_ckyc_dup_check`):
- Overload 1: CKYC number uniqueness across company
- Overload 2 (PJT-1517): PAN + CKYC combo cross-contact conflict validation

### 12.8 NRI Mandatory Check (PJT-320)

**Procedure:** `ps_pr_nri_mandatory_check`

For `residential_type = 'NRI'`:
- Visa date mandatory
- Foreign employer name, country, and address mandatory
- Foreign residence address mandatory
- Country code mandatory
- Foreign mobile number mandatory
- Foreign email ID mandatory

### 12.9 Deceased Customer Check (PJT-921)

For `deceased_indicator = 'Y'`:
- Date of death mandatory
- At least one legal heir must be recorded
- Full heir validation or death-date-only validation based on `check_ind` (`'H'` or `'D'`)

### 12.10 eKYC Validation (INC-132794)

**Procedure:** `ps_pr_val_ekyc_data`
- Returns `ekyc_appl` — whether eKYC is configured for this branch
- Returns `verified_source` — PAN, Aadhaar, etc.
- Returns `verification_status` — Verified / Not Verified / Pending

---

## 13. Caution List Screening

### 13.1 Data Sources

| Source | Load Procedure | Category Code | Description |
|---|---|---|---|
| CCDB (RBI) | `ps_pr_load_ccdb_ext_data` | `'E'` | External — Central Caution Database (XML) |
| UAPA | `ps_pr_load_ccdb_ext_uapa_data` | `'E'` | External — Unlawful Activities Prevention Act |
| UN Sanctions | (Within `ps_pr_load_ccdb_ext_data`) | `'E'` | United Nations Security Council sanctions |
| Internal | `ps_pr_load_ccdb_int_data` | `'I'` | Company defaulters/fraud list (Excel upload) |
| PAN Caution | `ps_pr_load_ccdb_int_pan` | `'I'` | PAN-specific blocking list (PJT-1754, Mar 2025) |

**Upload Modes:** `'O'` Overwrite / `'A'` Append (with duplicate check before insert).

### 13.2 Matching Algorithm

Priority order (LATERAL join, ORDER BY priority, FETCH FIRST ROW ONLY):

| Priority | Match Type | Logic |
|---|---|---|
| 1 | Passport Match | Exact passport number match |
| 2 | DOB + Name Match | Name fuzzy match AND DOB exact OR DOB range |
| 3 | Name-only Match | Name fuzzy match where DOB is NULL in caution record |

**Name fuzzy matching** uses three pre-computed normalised name variants on the caution master:
- `v_good_quality_name` — Cleaned, de-spaced version
- `v_low_quality_name` — Phonetic/variant
- `v_suspected_customer_name` — Original suspected name

Customer name normalisation: `regexp_replace(trim(contact_name3 || contact_name4 || contact_name5), '[[:space:]]+', '')`

### 13.3 Real-Time Screening at Enquiry/Prospect Stage

**Procedure:** `PS_PK_LN_CAUTION_CUST_DB.ps_pr_val_caution_list`

Called per applicant with:
- `customer_code`, `customer_name`, `cust_display_name`
- `reference_type = 'ENQUIRY'`, `reference_no = prospect_id`
- `customer_role` — Lessee / Co-Lessee / Guarantor
- `dob`, `passport`, `pan`, `aadhaar`
- `occupation`, `pincode`, `branch_code`

Internally calls:
1. `ps_pr_val_ext_caution_list` — CCDB / UN Sanctions
2. `ps_pr_val_int_caution_list` — Internal list (PAN + Aadhaar match included)
3. `ps_pr_val_oth_caution_list` — UAPA and other regulatory lists

**Outputs:**
- `caution_flag = 'Y'` → applicant is on a caution list (non-blocking; email triggered)
- `mail_ind = 'Y'` → email notification sent to branch manager

### 13.4 Hard Block Gates

| Gate | Source | Effect |
|---|---|---|
| PAN Caution (`ps_fn_pan_status_chk`) | PAN caution list (PJT-1754) | Hard block pre-insert; error LN5337 |
| Passport CC Database Check | External CC Database | Hard block; flag as "Undesirable Prospect" |

### 13.5 Scheduled Scrubbing

**Procedures:** `ps_pr_sch_ccdb_scrub_dtls` (schedule) / `ps_pr_ccdb_scrub_dtls` (execute)

Processes active lease portfolio for a given month/year against the caution master. Matches insert a row into `ps_tb_ln_ccdb_caution_list` with `reference_type = 'CUSTOMER'` and trigger email alerts.

### 13.6 Whitelist Management

**Procedure:** `ps_pr_ln_upd_ccdb_whitelist_ind`

Whitelisting requires documented justification. Once whitelisted, the customer passes caution checks without raising an alert. Email sent to stakeholders on whitelist status change.

### 13.7 Revocation

**Procedures:** `ps_pr_revoke_int_caution_list` (initiate) / `ps_pr_auth_revoke_dtls` (authorise — maker-checker)

---

## 14. Enquiry (Prospect) State Machine

### 14.1 Lead State Transitions

```
[Lead Created / Uploaded]
          │
          ▼
       New
          │ (Assignment)
          ▼
      Assigned
          │ (First interaction)
          ▼
    In-Progress
          │
     ┌────┴─────────┐
     ▼               ▼
  Promoted        Closed
  (to Prospect)  (Closure Reason)
```

**Lead Temperature:** Cold ↔ Warm ↔ Hot (independently tracked with audit trail).

### 14.2 Prospect (Enquiry) State Transitions

```
[Lead Promoted / Direct Creation]
          │
          ▼
        Draft
          │ (PAN/GST validation success)
          ▼
      Validated
          │ (Assignment + first interaction)
          ▼
        Active
          │ (Application initiated + KYC/AML/CAM started)
          ▼
     InAppraisal
          │ (KYC=Complete AND CAM=Approved)
          ▼
  CustomerCreated
          │
          ▼
 (Contract Configuration — out of scope)
          │
     (or at any stage)
          │
          ▼
        Closed
```

**Prospect status codes:**

| Code | Meaning |
|---|---|
| `DR` | Draft — created but PAN/GST validation not complete |
| `VL` | Validated — PAN/GST validated, AML initiated |
| `PE` | Active/Pending — assignment complete; interactions ongoing |
| `IA` | InAppraisal — KYC/CAM/AML in parallel |
| `CC` | CustomerCreated — Enterprise Customer ID generated |
| `APL` | Application created and active |
| `C` | Cancelled |
| `E` | Expired (no application within 45 days — INC-130274) |
| `B` | Blocked (AML / Caution hard block) |

**Modification rule:** Prospect can only be modified if no active application exists.

**Expiry rule:** Prospects without an application within 45 days are automatically expired by `ps_pr_enq_appl_expiry_mark` (changed from 65 days per INC-130274, April 2022).

### 14.3 Opportunity Status Transitions

| From | To | Trigger |
|---|---|---|
| Open | Quoted | Rack rate or customized quote attached |
| Quoted | Negotiation | Negotiation started |
| Negotiation | QuoteLocked | Quote finalized and locked |
| QuoteLocked | Converted | Application initiated |
| Any | Closed | Closure reason recorded |

### 14.4 Quote Status Transitions

| From | To | Trigger |
|---|---|---|
| Draft | Negotiation | Negotiation started |
| Negotiation | PendingApproval | Variance beyond band |
| PendingApproval | Approved | Approver approves |
| Approved | Locked | Finalized and linked to Offer/Contract |
| Locked | Expired | Validity period elapsed |
| Locked | Cancelled | Override via approval workflow |

---

## 15. Key Data Structures

### 15.1 Lead Master

| Attribute | Mandatory | Notes |
|---|---|---|
| `Temp Customer No.` | Yes | Format: `TMP-YYYY-NNNNNN` |
| `LeadRefNo` | Yes | Format: `LOB-YYYYMM-NNNNNN` |
| `Lead Type` | Yes | Individual / Commercial |
| `Company (Known As)` | Conditional | If Commercial |
| `Contact Person` | Conditional | If Commercial |
| `Source Category` | Yes | Internal / External / Campaign / Dealer |
| `Source Name` | Yes | e.g., `Diwali-CAR-LEASE-2025` |
| `Mobile` | Conditional | One identifier required |
| `Email` | Conditional | Multiple Email IDs supported |
| `PAN` | Optional at base; Mandatory at Prospect | Used for dedup |
| `GSTIN` | Optional at base; Mandatory at Prospect (Commercial) | Used for dedup |
| `UCIC` | Conditional | Displayed on identity match |
| `DedupLabel` | Yes | PossibleExisting / Unknown / Conflict / New |
| `Lead Temperature` | Yes | Cold / Warm / Hot |
| `StatusSuggestedBy` | Yes | System / Manual |
| `StatusReason` | Conditional | Mandatory on manual override |
| `ClosureReason` | Conditional | Mandatory on closure |
| `Geotag` | Optional | Latitude/Longitude (mobile only) |

### 15.2 Prospect (Enquiry) Header — `ps_tb_ls_prospect_hdr`

| Column | Type | Notes |
|---|---|---|
| `prospect_id` | VARCHAR2(25) | Business key; `PR-YYYY-NNNNNN` |
| `prospect_date` | DATE | Truncated to date |
| `prospect_status` | VARCHAR2(3) | DR/VL/PE/IA/CC/APL/C/E/B |
| `contract_type` | NUMBER | Finance Lease / Operating Lease |
| `asset_class_code` | NUMBER | Asset class being leased |
| `lob_type` | VARCHAR2 | `'LEASING'` |
| `pan_validated_ind` | VARCHAR2(1) | Y/N |
| `gstin_validated_ind` | VARCHAR2(1) | Y/N |
| `aml_status` | VARCHAR2 | Clear / Flagged / Blocked |
| `hunter_result` | VARCHAR2 | P/F/M/NULL |
| `cibil_status` | VARCHAR2 | Bureau submission status |
| `lead_ref_no` | VARCHAR2 | FK to Lead master |
| `temp_customer_no` | VARCHAR2 | Lead-stage temp customer |
| `org_branch_code` | VARCHAR2 | Original branch (preserved on transfer) |
| `group_lob_ind` | VARCHAR2(1) | Y for group/corporate leasing |
| `preferred_customer_ind` | VARCHAR2(1) | Y for preferred rate customers |
| `enquiry_source` | VARCHAR2 | B/M/A |
| `device_ind` | VARCHAR2 | D/M |

### 15.3 Prospect Detail — `ps_tb_ls_prospect_dtls`

One row per party (Lessee / Co-Lessee / Guarantor / BO):

| Column | Notes |
|---|---|
| `prospect_id` | FK to header |
| `applicant_sl_no` | 1 = Main Lessee, 2+ = Co-Lessees/Guarantors |
| `applicant_role` | LS/CL/GR (Lessee/Co-Lessee/Guarantor) |
| `contact_id`, `contact_code` | FK to contact master |
| `aml_case_id` | AML screening reference |
| `cibil_req_no`, `cibil_ref_id` | Bureau reference |
| `pan`, `aadhaar_no`, `passport_no` | KYC identifiers |
| `voter_id_no`, `driving_licence_no` | KYC identifiers |
| Address columns | `addr_line1`, `addr_line2`, `street_name`, `pincode`, `district`, `state_name` |
| `mobile_no`, `telephone_no` | Contact details |

### 15.4 Opportunity — `ps_tb_ls_opportunity`

| Column | Notes |
|---|---|
| `opportunity_id` | Format: `OPP-YYYY-NNNNN` |
| `prospect_id` | FK to Prospect header |
| `lob_type` | Leasing / Lending / Deposits / Others |
| `asset_category` | Mandatory |
| `asset_class` | Mandatory |
| `make`, `model_variant_type` | Optional |
| `potential_volume` | Units of potential business |
| `potential_value` | Value of potential business |
| `opportunity_status` | Open/Quoted/Negotiation/QuoteLocked/Converted/Closed |

### 15.5 Rack Rate Master — `ps_tb_ls_rack_rate_mst`

| Column | Notes |
|---|---|
| `rack_rate_code` | R######### (unique) |
| `rack_rate_version` | Version number |
| `effective_from`, `effective_to` | Validity window; no overlap per active version |
| `asset_class`, `asset_category` | Asset taxonomy |
| `lease_type` | Finance/Operating |
| `gst_rate`, `depreciation_rate` | Percentage values |
| `tenure_months` | Lease period |
| `rv_type`, `rv_pct_amt` | Residual value basis and value |
| `cof_rate`, `base_rate` | Cost of funds |
| `corporate_tax` | Corporate tax % |
| `atdr` | After Tax Discount Rate |
| `lmf_type`, `lmf_pct` | Lease Management Fee |
| `deposit_pct` | Security deposit % |
| `bdf`, `subvention` | Additional parameters |
| `rack_rate_status` | Draft/PendingApproval/Approved/Active/Inactive |
| `preferred_customer_ind` | Y for preferred company rates |

### 15.6 Quote — `ps_tb_ls_quote_dtls`

| Column | Notes |
|---|---|
| `quote_id` | Q######### (unique) |
| `rack_rate_code` | FK to Rack Rate master |
| `rack_rate_version` | Version used |
| `prospect_id` | FK to Prospect |
| `opportunity_id` | FK to Opportunity |
| `lessee_contact_id` | Lessee party |
| `period_type` | P (Primary) / S (Secondary) |
| `rental_frequency` | M/B/Q/H/Y |
| `payment_type` | AD/AR/MAD/MAR/SAD/SAR |
| `quarter_month_ind` | Calendar pattern for billing |
| `start_installment`, `end_installment` | Installment range |
| `quote_amount` | Rental/PMPL amount |
| `variance_pct` | Computed negotiation variance |
| `quote_status` | Draft/Negotiation/PendingApproval/Approved/Locked/Expired/Cancelled |
| `valid_from`, `valid_to` | Quote validity window |
| `approval_status` | Pending/Approved/Rejected |
| `approver_id`, `approval_remarks` | Approval audit |

### 15.7 Caution Master and Caution List

Same structure as lending (see `ps_tb_ln_ccdb_caution_master` and `ps_tb_ln_ccdb_caution_list`) with `reference_type = 'ENQUIRY'` for Prospect-level flags and `reference_type = 'CUSTOMER'` for scrub hits.

---

## 16. Error Codes and Messages

| Error Code | Type | Message / Meaning |
|---|---|---|
| `LN480` | E | Invalid Branch Code |
| `LN13` | E | User ID is Required |
| `GL461` | E | Invalid User |
| `LN5337` | E | PAN Blocked (Caution PAN — PJT-1754) |
| `LN3713` | E | Application Created for Enquiry — Cannot Modify |
| `LN5144` | E | Duplicate Found for Customer (Caution upload dedup) |
| `LN5145` | E | Invalid Name Format for Customer |
| `LN5154` | E | Invalid Date Format for Suspected Customer |
| `LN5155` | E | Invalid Passport Format for Suspected Customer |
| `GL492` | E | Duplicate Entry In Excel (Caution upload) |
| `LS001` | E | PAN Validation Failed — Legal name/address not fetched |
| `LS002` | E | GSTIN Validation Failed — Legal entity name/address not fetched |
| `LS003` | E | Passport Matched in CC Database — Undesirable Prospect; processing blocked |
| `LS004` | E | Name Matched in CC Database — Flagged as Undesirable Customer |
| `LS005` | E | AML True Match Confirmed — Prospect Blocked Pending AML Clearance |
| `LS006` | E | DedupLabel not resolved — Cannot promote Lead to Prospect |
| `LS007` | E | PAN/GST Mandatory for Prospect Promotion |
| `LS008` | E | Re-KYC Pending — Application/Payment Blocked |
| `LS009` | E | BOD Pending — Application/Payment Blocked |
| `LS010` | E | Quote Expired — Revalidation Required Before Contract Linkage |
| `LS011` | E | Quote Not Locked — Complete Approvals Before Linking to Contract |
| `LS012` | E | Rack Rate Effective Date Overlap — Adjust Dates or Create Separate Code |
| `LS013` | E | Rack Rate Not Approved — Cannot Activate |
| `LS014` | E | Negotiation Variance Exceeds Band — Approval Required |
| `LS015` | E | KYC Not Complete — Customer Creation Blocked |
| `LS016` | E | CAM Not Approved — Customer Creation Blocked |
| `LS017` | E | Lineage Break Detected — Cross-reference Missing |
| (System error via `ps_fn_get_error_text`) | S | System error — Contact Admin with log ID |

Error type `'E'` = Business/validation error (user-correctable).  
Error type `'S'` = System error (unexpected exception — requires admin investigation via `ps_tb_error_log`).

---

## 17. Integration Catalogue

| System | Direction | Module / Procedure | Protocol | Notes |
|---|---|---|---|---|
| PAN API (Income Tax) | Outbound | PAN Validation, `ps_pr_pan_dedup_val_api_call`, `ps_pr_pan_mandate_val_api_call` | HTTP REST | PJT-1517, PJT-943 |
| GST Portal API | Outbound | GSTIN Validation | HTTP REST | Legal name/address enrichment |
| AML Screening Engine (Enterprise) | Outbound | AML1–AML10 | HTTP REST / Batch | UNSC, UAPA, PEP, BO |
| CC Database (External Caution) | Outbound | Passport CC check, caution list load | XML / HTTP | Hard block on match |
| Hunter (Fraud Network) | Outbound | `ps_pr_req_hunter_xml`, `ps_pr_hunter_sherlock_xml` | HTTP XML/JSON | Non-blocking for Prospect save |
| Sherlock (Internal Fraud DB) | Outbound | `ps_pr_hunter_sherlock_xml` | HTTP JSON | Non-blocking |
| CIBIL Bureau | Outbound | `ps_pr_ln_iud_bureau_enq_dtls`, `ps_pr_ln_cibil_req_resp` | HTTP XML | Non-blocking |
| STP Engine (Python) | Outbound | `ps_pr_enq_stp_process` / `ps_pr_enq_stp_response` | HTTP JSON | Existing customers |
| SMS Gateway | Outbound | `ps_pr_enq_sms_to_customer`, `ps_pr_enquiry_sms_common` | SMS Gateway | Consent tracking |
| NEWGEN DMS | Outbound | `ps_pr_get_appl_doc_dtls` | API (INC-114143) | Document storage/checklist |
| Email (CCDB/AML alerts) | Outbound | `ps_pr_send_mail` | SMTP | Caution/AML hits; whitelist status |
| Pricing Engine | Internal | Rack Rate and Quote computation | In-process | IRR/rental schedule |
| CKYC Registry | Outbound | CKYC validation, `ps_pr_ckyc_dup_check` | HTTP API | Central KYC verification |
| WhatsApp / Notification | Outbound | AML7 Re-KYC reminders | WhatsApp gateway | Per RBI direction |
| Power BI / Reporting Layer | Outbound | AML10 quarterly reporting | Data pipeline | Enterprise AML MIS |

---

## 18. Special Scenarios and Business Rules

### 18.1 Group / Corporate Leasing (e.g., Microsoft, Infosys Fleet)

- Lead Type = `Commercial` with `group_lob_ind = 'Y'`.
- Preferred Rack Rates are configured in the Rack Rate master with `preferred_customer_ind = 'Y'` linked to the company code.
- At Quote creation, system checks whether the Prospect's company is a preferred entity and auto-applies the preferred Rack Rate.
- Multiple lessees under a single corporate arrangement are linked via UCIC.

### 18.2 Existing Customer — Prospect Starting Point

Where an existing customer is the subject of a new leasing enquiry and policy permits, the system can start at the Prospect stage (bypassing Lead creation). The existing UCIC and customer code are linked directly to the Prospect record, preserving lineage.

### 18.3 LOS Lite (PJT-986 — Simplified Enquiry Channel)

`enq_old_new_ind` flag differentiates fresh LOS Lite enquiry from conversion of existing one. Additional captured fields: PAN, mobile, Aadhaar, Passport, Voter ID, Driving Licence.

### 18.4 Branch Transfer

**Procedures:** `ps_pr_val_enquiry_br_transfer`, `ps_pr_ins_enq_br_transfer`

Prospect can be transferred to a different branch while preserving `org_branch_code`. Records: source branch, destination branch, source/destination marketing employees, mandatory remarks. Prospect ID unchanged.

### 18.5 Employee Transfer

**Procedures:** `ps_pr_get_enq_market_emp_trans`, `ps_pr_ins_enq_emp_transfer`

All pending Prospects assigned to a transferred employee can be bulk-transferred to the replacement employee.

### 18.6 Enquiry-to-Contract Conversion (e2c — CL2236)

**Procedures:** `ps_pr_ln_e2c_conv_support_save`, `ps_pr_val_e2c_dtls`

For cases where a Prospect should be directly converted to a contract (bypassing the application stage), e2c conversion is available for eligible constitution types.

### 18.7 Sanction Validity for Existing Customers

For existing lessees with an active sanction within validity period, only limit checking may be required (full CAM may not be needed). Sanction validity logic is configurable per product/segment policy.

### 18.8 Multi-LoB Prospects

A single Prospect may have Opportunities tagged to multiple Lines of Business (Leasing, Lending, Deposits). Each Opportunity follows its own lifecycle independently. All share the same Prospect identity and AML status.

### 18.9 Enquiry Expiry

Prospects without an active application within 45 days are automatically marked Expired (`status = 'E'`) by the `ps_pr_enq_appl_expiry_mark` scheduled job (changed from 65 days — INC-130274, April 2022).

### 18.10 Mobile Lead (Mode L)

- `device_ind = 'M'` on header
- `enquiry_source = 'M'`
- KYC validation uses `ps_pr_mob_val_kyc_dtls` (mobile-specific validation flow)
- `ps_pr_lead_processing_mail` sends processing confirmation email to branch

---

## 19. Audit and Logging

### 19.1 Transaction Audit

Every transaction inserts an audit log object `ps_ob_audit_log` containing:
- `created_by` / `modified_by` — User ID
- `created_date` / `modified_date`
- `product_id`, `machine_id`, `ip_address`, `component_code`

### 19.2 Error Logging

All errors logged to `ps_tb_error_log` via `ps_pk_error_log.ps_pr_insert_error_mesg`:
- `sqlcode`, `sqlerrm`
- `log_stage` — descriptive text indicating where in the procedure the error occurred
- Error type: `'E'` (business), `'S'` (system)

Global log ID (`ps_pk_global_variables.pv_n_log_id`) used as reference for system errors returned to caller.

### 19.3 Caution Upload Activity

Logged in `ps_tb_ln_ccdb_upload_log`:
- Start/end time
- Upload status: `'P'` Processing / `'C'` Completed / `'E'` Error
- Error message

### 19.4 AML Audit Trail

All AML screening events logged with:
- Screening timestamp, party screened, sanctions lists checked
- Match outcome (No Match / Partial / Full / True Match)
- Action taken (Flagged / Blocked / Whitelisted)
- AML Team queue/case reference

AML audit records are append-only.

### 19.5 Assignment Audit Trail

Every Lead/Prospect assignment/re-assignment records:
- Previous owner, new owner
- Reason code and remarks (mandatory)
- Timestamp
- SLA timer start/end and breach status

### 19.6 Pricing Audit Trail

Field-level changes (before/after values) for all rack rate and quote edits. Approver decisions with remarks and timestamps. Evidence attachments stored in DMS with audit reference.

---

## 20. Configuration Dependencies

| Configuration Table / Master | Used By | Purpose |
|---|---|---|
| `ps_tb_branch_defn` | Prospect Creation | Branch open/close dates, state code, segment |
| `ps_tb_employee_defn` | Prospect Creation | Employee type, department code/name |
| `ps_tb_user_defn` | Prospect Creation | User-to-employee mapping, user status |
| `ps_tb_ln_loan_enq_asset_ctrl` | Prospect Creation | Branch-level asset/contract type controls |
| `ps_tb_com_name_defn` | Contact Validation | Constitution + residential type → name field config |
| `ps_tb_ln_ccdb_caution_master` | Caution Screening | Caution list data (external + internal) |
| `ps_tb_ln_ccdb_caution_list` | Caution Screening | Flagged references |
| `ps_tb_ln_mpos_sms_url_dict` | SMS | SMS URL for document upload link (CL7006) |
| Rack Rate Master (`ps_tb_ls_rack_rate_mst`) | Pricing | Effective-dated rack rates per asset/lease type |
| Negotiation Band Config | Pricing | Authority matrix variance thresholds per parameter |
| AML Risk Matrix | AML | Risk categorisation rules per constitution/exposure |
| Re-KYC Frequency Config | AML | Re-KYC due date computation per risk category |
| SLA Config | Lead/Prospect | Assignment SLA timers and escalation matrix |
| Adoption Policy Flag | Pricing | Controls whether parameter changes apply to existing quotes |
| Control Number Config | Prospect/Quote | Format for Prospect ID and Quote ID generation |

---

## 21. Business Requirements Traceability

| Requirement ID | Title | Source RS | Covered In |
|---|---|---|---|
| REQ-LD-001 | Temp Customer No. & LeadRefNo generation | Lead RS | Section 4.3 |
| REQ-LD-002 | Lead source capture | Lead RS | Section 4.3 |
| REQ-PR-001 | Excel template upload | Lead RS | Section 4.4 |
| REQ-IN-001 | API ingestion | Lead RS | Section 4.4 |
| REQ-VC-001 | Mandatory identifier rule | Lead RS | Section 4.3 |
| REQ-PR-002 | Default ownership to CPU | Lead RS | Section 4.3 |
| REQ-PR-003 | Self-assign on creation | Lead RS | Section 4.3 |
| REQ-PR-004 | Hierarchy assignment up to 4 levels | Lead RS | Section 4.6 |
| REQ-VC-002 | Remarks mandatory on assignment | Lead RS | Section 4.6 |
| REQ-VC-003 | Deduplication by identifiers | Lead RS | Section 4.5 |
| REQ-LD-010 | UCIC mapping visibility | Lead RS | Section 4.5 |
| REQ-PR-010 | Exception Queue | Lead RS | Section 4.4, 4.5 |
| REQ-LD-020 | Call log capture | Lead RS | Section 4.7 |
| REQ-LD-021 | Next meeting scheduling | Lead RS | Section 4.7 |
| REQ-PR-030 | Lead status classification | Lead RS | Section 4.8 |
| REQ-PR-040 | Prospect promotion rule | Lead RS | Section 4.9 |
| REQ-PR-041 | Closure rules | Lead RS | Section 4.9 |
| REQ-RP-001 | To-do list dashboard | Lead RS | Section 4.10 |
| REQ-RP-002 | Performance dashboard | Lead RS | Section 4.10 |
| REQ-PR-001 (Prospect) | Prospect creation post validation | Prospect RS | Section 5.3 |
| REQ-VC-001 (Prospect) | PAN validation for individuals | Prospect RS | Section 6.1 |
| REQ-VC-002 (Prospect) | GSTIN validation for non-individuals | Prospect RS | Section 6.2 |
| REQ-PR-010 (Prospect) | Opportunity creation per prospect | Prospect RS | Section 8.2 |
| REQ-PR-011 | Multi-LoB opportunities | Prospect RS | Section 8.2 |
| REQ-QT-001 | Rack rate quote | Prospect RS | Section 9.2 |
| REQ-QT-002 | Customized quote & approvals | Prospect RS | Section 9.3 |
| REQ-QT-004 | Quote lock | Prospect RS | Section 9.3.3 |
| REQ-AP-001 | Application initiation from prospect | Prospect RS | Section 10.2 |
| REQ-AP-002 | Parallel KYC & CAM | Prospect RS | Section 10.2 |
| REQ-PR-030 (Prospect) | Customer creation gate | Prospect RS | Section 11.1 |
| REQ-LD-020 (Prospect) | Lineage links | Prospect RS | Section 11.3 |
| AML1 | Name Screening | Prospect RS (6A) | Section 7.2 |
| AML2 | PII Capture | Prospect RS (6A) | Section 7.3 |
| AML3 | Beneficial Owner Capture | Prospect RS (6A) | Section 7.4 |
| AML4 | AML Risk Profiling | Prospect RS (6A) | Section 7.5 |
| AML5 | Re-KYC Maintenance | Prospect RS (6A) | Section 7.6 |
| AML6 | Re-KYC Gate | Prospect RS (6A) | Section 7.7 |
| AML7 | Re-KYC Reminders | Prospect RS (6A) | Section 7.8 |
| AML8 | BOD Gate | Prospect RS (6A) | Section 7.9 |
| AML9 | Auto Risk Categorisation Batch | Prospect RS (6A) | Section 7.10 |
| AML10 | AML Reporting | Prospect RS (6A) | Section 7.11 |
| REQ-PM-001 | Rack rate code creation | Pricing RS | Section 9.2 |
| REQ-PM-002 | Rack rate versioning & effective dating | Pricing RS | Section 9.2.2 |
| REQ-VC-001 (Pricing) | Maker-checker enforcement | Pricing RS | Section 9.5 |
| REQ-QT-001 (Pricing) | Quote ID generation & versioning | Pricing RS | Section 9.3 |
| REQ-QT-003 | Negotiation band controls | Pricing RS | Section 9.3.2 |
| REQ-QT-004 | Quote validity window | Pricing RS | Section 9.3.3 |
| REQ-PM-010 | Bulk rack rate revision | Pricing RS | Section 9.4 |
| REQ-PR-010 (Pricing) | Selective adoption rules | Pricing RS | Section 9.4 |
| REQ-RP-001 (Pricing) | Rack rate register | Pricing RS | Section 9.5 |
| REQ-RP-002 (Pricing) | Quote register & deviation log | Pricing RS | Section 9.5 |
| Sanction screening name logic | Name matching rules | AML Sanction doc | Section 7.2.3 |
| Passport CC DB check | Passport blocking | Prospect RS (P3) | Section 5.3 Step 6 |

---

*End of Document*
