# Leasing Lead Generation & Prospect Creation
## Detailed Functional Specification

| Document Control | |
| --- | --- |
| Version | 2.0 |
| Date | 2026-05-07 |
| Status | Draft |
| Author | TCS Team |
| Business Domain | Leasing — Lead-to-Prospect |
| Reference Documents | Lead_Generation_RS_v1.0, Prospect_Creation_RS_v1.0, Consolidated_UI_PLSQL_FunctionalSpec_v2, LoanEnquiryGeneration_ui_plsql_consolidated_FunctionalSpecification |
| Supersedes | Leasing_Lead_Prospect_Functional_Specification.md v1.0 (2026-04-24) |

---

## Changes from v1.0

This version incorporates a gap analysis between the leasing specification and the updated lending functional specification (Consolidated_UI_PLSQL_FunctionalSpec_v2). All v1.0 content is preserved verbatim. The following additions and enhancements are introduced:

| Change | Scope | Priority |
| --- | --- | --- |
| Mobile number validation rules (COM112–COM115) | LP2, Section 8 | Critical |
| CKYC number capture and uniqueness check (COM105) | LP2, LP4, Section 8, 10 | Critical |
| Source-of-business registration restriction (Rule 55 equivalent) | LP2 | Critical |
| Digital-channel consistency rule (Rule 54 equivalent) | LP2, Section 8 | Critical |
| Autonomous transaction logging (Rule 25 equivalent) | LP6, PP4, PP7 | Critical |
| One-branch restriction for leads and prospects (LN5098/LN5099) | LP8 | Critical |
| Branch activation date guard | LP2, PP3 | Critical |
| APC/ANC enquiry decision codes on closure (LN4926) | LP8, PP8 | Critical |
| Caution-list email alert on match (Rules 59/60 equivalent) | LP4, PP7 | Critical |
| CIBIL re-initiation control rules (LN3786/LN3780/LN3782) | PP7 | Critical |
| Two-stage branch email on application creation (Rule 57 equivalent) | PP7 | High |
| Aged-pending day-of-month trigger mechanism | Section 8, Section 15 | High |
| OEM exemption named list (MSIL/TMF/TAFE/HYUNDAI) | LP4 | High |
| Internal KYC dedup error codes (LN4084/LN4088/LN3712) | LP4 | High |
| Fraud block status codes (FA/F) in applicant status | LP4, PP7 | High |
| Full customer profile display on dedup match | LP4 | High |
| Three-check gate before Prospect promotion (Rule 52 equivalent) | LP8 | Medium |
| Vruddhi/special contract type codes (12/22/41/42/43/44) | PP6, PP7 | Medium |
| 35 km default for geographic service area | LP2, PP3, Section 15 | Medium |
| COM103 Aadhaar validation response code | LP4, Section 8 | Medium |
| High-level functional requirements section (Section 0) | Document | Medium |
| Process end states table (Section 4) | Document | Medium |
| Branch entity table (Section 10.4) | Section 10 | Medium |
| New configuration parameters reference (Section 15) | New section | Medium |
| Omissions and out-of-scope rules note (Section 16) | New section | Medium |
| New glossary terms: CKYC, APC, ANC, Autonomous Transaction, Vruddhi | Section 14 | Medium |
| New exceptions EX-31 through EX-45 | Section 9 | All priorities |

---

## Table of Contents

0. High-Level Functional Requirements
1. Summary & Objectives
2. Scope
3. Key Stakeholders
4. Processes Covered
5. Process Flow — Lead Generation
6. Process Flow — Prospect Creation
7. Business Rules
8. Validation Rules
9. Exception Handling
10. Data Entities & Attributes
11. Status Models & Transitions
12. Integration Touchpoints
13. Dashboards, SLAs & KPIs
14. Glossary
15. Configuration Parameters Reference
16. Omissions & Out-of-Scope Notes

---

## 0. High-Level Functional Requirements

This section summarises the top-level functional requirements that govern the entire lead-to-customer lifecycle in the Leasing domain.

| Req ID | Requirement | Priority |
| --- | --- | --- |
| HLR-01 | The system shall allow authenticated users to create leasing leads from mobile, desktop, bulk upload, and API channels. | Must Have |
| HLR-02 | Every lead must have a unique, sequentially generated LRN produced by lock-protected atomic increment. | Must Have |
| HLR-03 | Identity de-duplication (Mobile/Email/PAN/GSTIN/CKYC) must be performed before a lead progresses beyond creation. | Must Have |
| HLR-04 | Every lead must be assigned to a user or team at all times; no lead shall remain unassigned. | Must Have |
| HLR-05 | All interactions (calls, emails, meetings) must be logged immutably with timestamp and outcome. | Must Have |
| HLR-06 | Lead temperature (Hot/Warm/Cold) must be system-maintained with configurable degradation and manual override. | Must Have |
| HLR-07 | Prospect creation requires PAN (individual) or GSTIN (commercial) validation via external service. | Must Have |
| HLR-08 | Opportunities, quotes, and applications are tracked per line of business with full version history. | Must Have |
| HLR-09 | Customer creation is gated on KYC = Complete AND CAM = Approved simultaneously. | Must Have |
| HLR-10 | Full immutable lineage (Lead → Prospect → Opportunity → Quote → Application → Customer) must be stored and queryable. | Must Have |
| HLR-11 | All KYC documents must be uploaded to the Document Management System with environment-appropriate routing. | Must Have |
| HLR-12 | Fraud screening (Hunter/Sherlock) and credit bureau (CIBIL) must be invoked per policy; failures must not block saves. | Must Have |
| HLR-13 | Role-based access control must restrict visibility of leads, prospects, and applications to the assigned hierarchy. | Must Have |
| HLR-14 | Comprehensive dashboards and KPIs must be available to all relevant roles per Section 13. | Must Have |
| HLR-15 | All business events must generate auditable records with operator, timestamp, and reason captured. | Must Have |

### Process End States

| Process | Terminal State(s) | Non-Terminal States |
| --- | --- | --- |
| Lead | Promoted, Closed | New, Assigned, In-Progress |
| Prospect | CustomerCreated, Closed | Draft, Validated, Active, InAppraisal |
| Opportunity | Won, Closed | Open, Quoted, Negotiation, QuoteLocked, ApplicationInitiated, InAppraisal, Sanctioned |
| Quote | Locked (final) | Draft, Shared, Negotiation |
| Application | Approved, Rejected | Initiated, InReview, Pending |
| Customer | Active (ongoing) | Created |

---

## 1. Summary & Objectives

This Functional Specification (FS) defines the end-to-end processing logic, business rules, validations, and controls for **Leasing Lead Generation** and **Leasing Prospect Creation**. It combines the requirement frameworks established in the leasing Requirement Specifications (Lead_Generation_RS_v1.0 and Prospect_Creation_RS_v1.0) with the detailed, proven business rules derived from the **Loan Enquiry Generation functional specification** — adapted for the leasing domain.

**Business purpose:** Enable a field officer, call centre associate, or authorised system user to create a Leasing Lead from multiple channels, qualify it through structured interaction tracking, and promote it to a fully validated Prospect with identity verification, opportunity creation, and downstream application origination — all governed by consistent identity, eligibility, geographic, and fraud controls.

**Triggered by:** A field officer, marketing employee, call centre associate, or authorised API/system submitting leasing lead details via mobile, desktop, bulk upload, or API channel.

**Key outcomes:**
- A validated Lead Reference Number (LRN) is generated for each new lead with minimum contact identifiers.
- De-duplication and existence checks using Mobile/Email/PAN/GSTIN/CKYC with UCIC mapping are performed before the lead progresses.
- A unique Prospect ID is generated only after successful PAN/GSTIN validation and legal name/address enrichment.
- Opportunity creation (per Line of Business), quote generation, and parallel KYC & Credit Appraisal (CAM) are supported from the Prospect stage.
- Customer creation is gated on KYC completion and CAM approval, with full lineage: Lead → Prospect → Opportunity → Quote → Application → Customer.

---

## 2. Scope

### In Scope
- Base lead record creation (manual — mobile and desktop).
- Lead ingestion via Excel bulk upload and API.
- Identity capture (PAN/GSTIN/CKYC) at Lead stage (optional) and Prospect stage (mandatory).
- De-duplication and existing-customer routing using Mobile/Email/PAN/GSTIN/CKYC and UCIC mapping.
- Hierarchical lead and prospect assignment and re-assignment (up to 4 levels) with SLA and audit trail.
- Interaction logging (calls/messages/emails/meetings) with next-action scheduling, including autonomous system-generated log entries.
- Lead status classification (Cold/Warm/Hot) with system suggestion and manual override.
- Promotion of qualified lead to Prospect, or closure with APC/ANC reason codes.
- PAN/GSTIN validation with legal name and address enrichment at Prospect stage.
- Opportunity creation per Line of Business with asset details and contract type classification.
- Quote generation (rack rate and customised) with negotiation and locking.
- Application origination from Prospect with parallel KYC and CAM tracking.
- CIBIL re-initiation control per policy.
- Enterprise Customer creation upon KYC = Complete and CAM = Approved.
- Role-based dashboards, SLA monitoring, and to-do lists.

### Out of Scope
- End-to-end Lease Contract configuration (covered in Lease Contract RS).
- Detailed KYC workflow design beyond initiation and status tracking.
- Pricing engine parameter governance beyond operational inputs.
- Campaign management beyond capturing Source Category and Source Name.
- Telephony/WhatsApp provider-specific integrations beyond interaction logging.
- CKYC registry integration beyond number capture and uniqueness check.

---

## 3. Key Stakeholders

| Stakeholder / Role | Primary Responsibilities | Key Outputs |
| --- | --- | --- |
| Central Processing Team (CPU) | Lead ingestion, default ownership, assignment to Branch/Call Centre, exception queue ownership, SLA monitoring | Assigned leads, exception resolution, escalation records |
| Call Centre Associate | Initial follow-up, qualification, interaction logging, status suggestion | Call logs, next meeting schedules, status updates |
| Branch Manager | Assign/re-assign within branch hierarchy, approve/record re-assignment reasons, ensure SLAs | Owner allocations, remarks, escalations |
| Field Officer / Sales Associate | Create leads, perform follow-up, create prospects and opportunities, generate quotes, initiate applications | Updated contact data, interactions, opportunities, quotes, application initiation |
| Operations Officer | Process governance, data quality controls, audit readiness | Validated lead/prospect records, control reports |
| Credit Officer / CAM | Run credit appraisal, provide sanctioned limits and conditions | CAM approval/decline, limits, conditions |
| Business Leadership (RVP/Regional/Branch Heads) | Performance monitoring, KPIs, pipeline governance | Dashboards, MIS, SLA compliance summary |

---

## 4. Processes Covered

| Process ID | Stage | Process Name | Outcome |
| --- | --- | --- | --- |
| LP1 | Lead | User & Device Authentication | Only authenticated users and registered devices may create or modify records |
| LP2 | Lead | Base Lead Record Creation (Manual) | Base lead created with Temp Customer No. and Lead Reference Number |
| LP3 | Lead | Lead Ingestion (Bulk Upload / API) | Leads ingested from Excel/API; invalid records routed to exception queue |
| LP4 | Lead | Identity Capture & Existence Check (De-duplication) | PAN/GSTIN/CKYC optionally captured; DedupLabel assigned; conflicts flagged |
| LP5 | Lead | Lead Assignment & Re-assignment (up to 4 levels) | Lead ownership assigned; re-assignment tracked with remarks and SLA |
| LP6 | Lead | Interaction Logging & Next Action Scheduling | Calls/messages/emails/meetings logged with timestamps and next follow-up; autonomous system entries generated |
| LP7 | Lead | Lead Qualification & Status Classification | Cold/Warm/Hot assigned (system-suggested + manual override) |
| LP8 | Lead | Move to Prospect / Closure | Lead promoted to Prospect with mandatory PAN/GSTIN, or closed with APC/ANC code |
| PP1 | Prospect | Prospect Interface — View Confirmed Lead & History | Assigned users view prospect with complete lead lineage |
| PP2 | Prospect | Prospect Assignment & Re-assignment Workflow | Prospect ownership assigned and re-assigned with audit trail |
| PP3 | Prospect | PAN/GSTIN Validation & Legal Name/Address Enrichment | Identity validated; legal name and registered address populated |
| PP4 | Prospect | Meeting & Interaction Management | Meetings/calls logged against prospect with reminders and history |
| PP5 | Prospect | Opportunity Creation (per LoB) and Tagging | Opportunity IDs created per LoB; asset details and contract type captured |
| PP6 | Prospect | Quote Generation (Rack Rate / Customised) & Negotiation | Quotes generated, negotiated, and locked; Vruddhi types applied where applicable |
| PP7 | Prospect | Application Origination (Parallel KYC & CAM) | Application processing initiated; KYC and CAM run in parallel; CIBIL re-initiation governed |
| PP8 | Prospect | Customer Creation & Lineage | Enterprise Customer created upon KYC Complete and CAM Approved; APC/ANC codes set |

---

## 5. Process Flow — Lead Generation

### LP1. User and Device Authentication

#### i. Process Definition
Every request to create or modify a lead record must first authenticate the requesting user's identity and validate the device through which the request is submitted.

#### ii. Prerequisites
User credentials and device identifiers are present in the request payload. The system registry is accessible.

#### iii. Process Details
The system receives the user identifier and device credentials. If the primary device identifier (IMEI) is absent, the secondary device identifier is substituted. The authentication service is invoked to verify:
- The user is **active** in the system registry.
- The device is **registered and authorised** for the user.

If the user status is not 'Active', or if the authentication service returns an error, the process immediately returns a failure response to the caller. No lead record is created or modified.

For mobile channel submissions, GPS coordinates are captured if the GPS-enabled indicator is active; the geotag (latitude/longitude) is stored against the lead record.

#### iv. Touchpoints
Authentication service; Employee register; Device registry.

#### v. Business Rules
- **Rule LP1.1:** Every request must be authenticated before any business processing begins. Authentication failure immediately rejects the submission — no data is written.
- **Rule LP1.2:** If the primary device identifier is blank, the secondary device identifier is substituted. If both are absent, the submission proceeds without device validation (legacy compatibility mode — configurable per deployment).
- **Rule LP1.3 (Employee Record Required):** The authenticated user must have an active record with an associated employee entry. If no active user with an employee record is found, error GL461 (Invalid User) is raised and all processing halts. This check is enforced within the record persistence routine after authentication succeeds.
- **Rule LP1.4 (GPS Capture):** GPS coordinates are captured and stored when the GPS-enabled indicator is active. GPS failure does not block submission.

---

### LP2. Base Lead Record Creation (Manual)

#### i. Process Definition
Create a base lead record via mobile or desktop form by a Field Officer, Call Centre Associate, Branch user, or Central Processing Team with minimum contact identifiers.

#### ii. Prerequisites
Creator has an active, authenticated session; at least one mandatory identifier is available (Name + one of Mobile/Email/PAN/GSTIN/Address); the target branch is active and past its activation date; the creator's source of business is registered in the system.

#### iii. Process Details

**Step 1 — Input Completeness Check:** The lead input payload is trimmed of whitespace. If the payload is absent after trimming, the process returns a failure response: "Lead Input Values Is Required."

**Step 2 — Lead Type Capture:** Capture Lead Type: Individual or Commercial.
- For Commercial leads, Company (Known As) and Contact Person are mandatory.
- For Individual leads, Applicant Name is mandatory.

**Step 3 — Source Capture:** Capture Source Category (Internal/External/Campaign/Dealer) and Source Name (e.g., Diwali-CAR-LEASE-2025). Both are mandatory at creation.

**Step 3A — Source-of-Business Validation (NEW v2.0):** The user's source-of-business code must be registered and active in the source-of-business master. If the source code is absent, expired, or not authorised for the branch, the submission is rejected with error equivalent to Rule 55 — "Source of Business not registered or not authorised." This check is applied before lead creation proceeds.

**Step 4 — Identifier Validation:** At least one of Mobile/Email/PAN/GSTIN/Address must be provided along with Name. Records that fail this rule are routed to the Exception Queue rather than creating a lead record.

**Step 5 — Applicant Record Preparation:** For each applicant (Individual lead or Commercial contact person):
- Gender is normalised: 'Male' → 'M'; all other values → 'F'. For Non-Individual (Commercial) constitution types, gender is cleared.
- Date of Birth is parsed from DD/MM/YYYY if provided.
- PAN exemption flag is set to 'Y' if a Declaration Account Number (DAN) is provided; otherwise 'N'. A blank DAN is treated as absent.
- Additional co-applicants (co-lessees) are sequentially labelled: MAIN APPLICANT, ADDL APPLICANT - 1, ADDL APPLICANT - 2, etc.
- Mobile number format is validated per Rules LP2.9a–LP2.9d below.
- CKYC number, if provided, is captured and validated for uniqueness per Rule LP2.11.

**Step 6 — Temporary Identity Assignment:** Generate:
- **Temp Customer Number** (format: TMP-YYYY-NNNNNN) — uniqueness enforced; annual-reset controlled by system parameter.
- **Lead Reference Number (LRN)** (format: LOB-YYYYMM-NNNNNN) — generated by atomically locking and incrementing a central control record. If the control record is locked by another user for more than 15 seconds, error D283 is returned: "This Document Number Is Being Used By Another Person."

**Step 7 — Default Assignment:** Unless the creator selects self-assign, the lead is assigned by default to the Central Processing Team. No lead shall remain unassigned after creation.

**Step 8 — Geographic Validation (Conditional):** If a pincode is provided in the lead record, it is validated against the assigned branch's geographic service area using the Pincode-Branch mapping table:
- The system computes the distance between the applicant's pincode location and the branch. The **default maximum service radius is 35 km** unless a branch-specific override is configured.
- If the distance exceeds the branch's configured maximum, the system checks whether the pincode is explicitly mapped to the branch in the override table.
- Validation is bypassed for SME branches (segment code 'SM'), users holding the designated bypass access right, and leads on the explicit exclusion list.
- **Mobile channel:** a geographic mismatch returns an informational warning (error LN3955 type 'I') with a save-allowed indicator of 'Y' — the user may acknowledge and proceed.
- **All other channels:** a geographic mismatch is a hard error (error LN3955 type 'E') and the lead cannot be saved.

**Step 8A — Branch Activation Date Check (NEW v2.0):** Before saving the lead, the system verifies that the target branch's activation date is on or before the current processing date. If the branch has not yet been activated, error equivalent to LN5099 is raised and the submission is rejected.

**Step 9 — Geotag Storage:** If the submission originated from a mobile device with GPS active, the latitude and longitude are stored against the lead record.

**Step 10 — Digital Channel Consistency Check (NEW v2.0):** If the lead channel is classified as a digital channel (e.g., mobile app, web portal, API), the selected lease/contract type must be within the set of lease types permitted for digital origination as configured in the system parameter registry. If the selected type falls outside the digital-permitted set, error equivalent to Rule 54 is raised: "Use the Digital Lease system for this lease type."

#### iv. Touchpoints
Lead Master; Assignment module; Interaction module (enabled post-creation); Number generation control record; Pincode-Branch mapping table; Source-of-business master; Branch activation date register.

#### v. Business Rules
- **Rule LP2.1 (Input Mandatory):** Lead input payload is mandatory. Rejection message: "Lead Input Values Is Required."
- **Rule LP2.2 (Minimum Identifier):** Name + at least one of Mobile/Email/PAN/GSTIN/Address is mandatory. Failure routes to Exception Queue, not immediate rejection.
- **Rule LP2.3 (LRN Sequential Generation with Lock Protection):** LRN generated by locking a central control record, incrementing atomically, and constructing the number as: LOB prefix + 4-digit branch + 2-digit year + padded sequence. Lock timeout (15 s) returns error D283.
- **Rule LP2.4 (Default Ownership):** All created leads default to Central Processing Team unless self-assign is explicitly selected by the creator (role-based permission required).
- **Rule LP2.5 (Gender Normalisation):** 'Male' → 'M'; all others → 'F'. Commercial applicants: gender cleared.
- **Rule LP2.6 (DAN / PAN Exemption):** PAN exemption flag = 'Y' if DAN provided; DAN must exist in system and must not have been previously utilised. Error LN4468 (invalid DAN) or LN4470 (already used) is raised on failure.
- **Rule LP2.7 (Sequential Applicant Numbering):** Additional co-lessees are sequentially numbered in their type label: MAIN APPLICANT, ADDL APPLICANT - 1, ADDL APPLICANT - 2, etc. The main lessee is always labelled MAIN APPLICANT.
- **Rule LP2.8 (Geographic Validation at Lead Creation):** If a pincode is provided at lead creation, it is validated against the branch's geographic service area. Default maximum radius is 35 km; branch-specific override applies. Mobile channel failures return an informational warning (error LN3955, save-allowed = 'Y'). All other channel failures are hard errors. Validation bypassed for SME branches, users with bypass access right, and leads on the explicit exclusion list.
- **Rule LP2.9 (Mobile Number Validation — NEW v2.0):** Mobile numbers are subject to the following checks before the lead record is accepted:
  - **LP2.9a (COM112 — Country Code):** If a country code prefix is provided, it must be a valid registered country code. An unrecognised country code raises COM112.
  - **LP2.9b (COM113 — Format):** The mobile number, after stripping any country code prefix, must conform to the standard 10-digit Indian mobile format (starting with 6–9). Invalid format raises COM113.
  - **LP2.9c (COM114 — Numeric Only):** The mobile number must contain only numeric characters (no spaces, dashes, or letters). A non-numeric mobile string raises COM114.
  - **LP2.9d (COM115 — Minimum Length):** The mobile number must meet the minimum digit length for the declared country. Insufficiently short numbers raise COM115.
  These mobile validation checks apply to both the primary mobile and any alternate mobile number fields.
- **Rule LP2.10 (Source-of-Business Registration — NEW v2.0):** The submitting user's source-of-business code must be active and authorised for the target branch in the source-of-business master before a lead can be created. Unregistered or unauthorised source codes cause the submission to be rejected. This rule applies to manual creation, bulk upload, and API ingestion channels.
- **Rule LP2.11 (CKYC Capture and Uniqueness — NEW v2.0):** If a CKYC number is provided at lead creation:
  - The CKYC number must conform to the standard CKYC registry format.
  - The system checks the CKYC number against existing lead, prospect, and customer records. If the CKYC number is already linked to a different identity record, error COM105 is raised — "CKYC number already registered to another customer."
  - CKYC is optional at Lead stage; it becomes part of the identity de-duplication set alongside PAN/GSTIN/Mobile/Email.
- **Rule LP2.12 (Branch Activation Date — NEW v2.0):** The target branch's activation date must be on or before the current processing date. If the branch activation date is in the future, the lead creation is rejected with error LN5099 equivalent: "Branch not yet active for processing."
- **Rule LP2.13 (Digital Channel Lease Type — NEW v2.0):** For leads originating through a digital channel, the selected lease/contract type must appear in the system-configured set of lease types permitted for digital origination. A non-permitted type raises an error equivalent to Rule 54: "Use the Digital Lease system for this lease type." This list is configurable in the system parameter registry (see Section 15).

---

### LP3. Lead Ingestion (Bulk Upload / API)

#### i. Process Definition
Ingest lead records from Excel template uploads or external/internal API payloads.

#### ii. Prerequisites
Source is registered; upload template is approved; API authentication is configured; submitting source-of-business code is registered for the target branch.

#### iii. Process Details

**Excel Upload:** Validate each row against the mandatory field set (Name + one identifier + Source Category + Source Name). Valid rows create lead records with auto-generated LRNs and default assignment to Central Processing Team. Invalid rows are placed into the Exception Queue with standardised reason codes.

**API Ingestion:** Validate each payload for authentication credentials, mandatory identifiers, and schema compliance. Records that pass are created with LRNs. Records that fail are routed to the Exception Queue with reason codes.

**Exception Queue:** Each exception record captures: reason code, field(s) in error, source file/batch identifier, exception ownership (Central Processing Team), and cure SLA deadline.

#### iv. Touchpoints
Upload service; Integration layer; Exception Queue module; Source-of-business master.

#### v. Business Rules
- **Rule LP3.1:** Upload file or API payload must contain at least: Name + one identifier + Source Category. Otherwise the record goes to Exception Queue — not a global rejection.
- **Rule LP3.2:** There are no unassigned leads after successful ingestion. All ingested leads default to Central Processing Team.
- **Rule LP3.3:** LRN generation for bulk records follows the same lock-protected sequential generation logic as manual creation (Rule LP2.3).
- **Rule LP3.4:** Exception Queue entries must have mandatory reason codes and are owned by Central Processing Team with configurable cure SLAs.

---

### LP4. Identity Capture & Existence Check (De-duplication)

#### i. Process Definition
Optionally capture PAN/GSTIN/CKYC at the Base Lead stage; perform KYC format validation, existence check, and de-duplication to identify potential existing customers or duplicates.

#### ii. Prerequisites
Lead record exists; identity inputs (Mobile/Email/PAN/GSTIN/CKYC) available.

#### iii. Process Details

**KYC Format Validation:**
- PAN must conform to the standard alphanumeric format (AAAAA9999A).
- Aadhaar number (if provided) must pass the **Verhoeff check-digit algorithm**: the digit string is reversed, processed through permutation and multiplication tables, and the computed check value must equal zero. Additionally, for individual lessees, Aadhaar must be exactly 12 digits. Verhoeff failure raises error **COM103**.
- Passport number and Voter ID must conform to their respective standard formats.

**Caution List Check:** If a PAN is provided, it is validated against the internal caution and block list. If the PAN status is anything other than 'Allowed', the lead record cannot progress and error LN5337 is raised with the specific caution reason. **Upon any caution-list match, an email alert is automatically dispatched to the designated caution-list monitoring team (Rules 59/60 equivalent). Email dispatch failure is logged silently and does not block transaction processing.**

**KYC De-duplication:**
For each non-null identity document (PAN, Driving Licence, Passport, Voter ID, Aadhaar, CKYC):
- Document format is validated before de-duplication is attempted.
- A de-duplication check is performed against existing lead, prospect, and application records.
  - If an internal duplicate is found, the system raises the appropriate internal error code: **LN4084** (duplicate PAN in lead), **LN4088** (duplicate PAN in application), or **LN3712** (duplicate KYC document across records).
- If a confirmed duplicate is found linked to a pending or active application, the process halts with a hard error.
- **On any de-duplication match, the system displays the full customer profile** associated with the matched record — including name, existing IDs, branch, assigned officer, and current status — so the processing team can make an informed routing decision.
- For individual lessees, PAN is submitted to an **external de-duplication service** (unless the lead originates from a recognised OEM/Partner source — see Rule LP4.10 for the named exemption list):
  - COM110 (confirmed duplicate) → hard rejection.
  - COM111 (potential duplicate) → informational warning; halted for review.
  - COM66 (soft warning) → recorded; processing continues.
- CKYC number, if provided, is included in the de-duplication set alongside PAN/GSTIN/Mobile/Email.

**Existence Check & UCIC Mapping:**
Run de-duplication against existing customer systems using Mobile/Email/PAN/GSTIN/CKYC. Assign DedupLabel:
- **PossibleExisting** — one identifier matches a known UCIC; route to Central Processing Team for fast-track assignment to existing account owner.
- **New** — no match found.
- **Unknown** — insufficient identifiers to determine.
- **Conflict** — PAN/GSTIN/CKYC maps to multiple UCICs; flag for exception/review queue with mandatory resolution before Prospect promotion.

Where a match is found, display the UCIC and the list of mapped customer codes, along with the full customer profile.

**Customer Risk Category:** When an existing customer is identified during de-duplication, validate the customer's risk category. If the risk category is restricted, the submission is halted and an error is raised.

**Existing Customer Routing:** If DedupLabel = PossibleExisting, the system alerts the Central Processing Team to consider routing the lead to the branch/officer already managing the existing account.

**Fraud Status on Applicant Records:** If any applicant record carries a fraud status code of **'FA'** (fraudulent applicant) or **'F'** (fraud flag active), the lead is immediately halted and routed for Central Team review. These fraud status codes are checked in addition to the Hunter/Sherlock screening at application stage.

#### iv. Touchpoints
Customer identity service; UCIC mapping service; Dedup engine; External PAN dedup API; Caution list database; CKYC registry; Email alert service.

#### v. Business Rules
- **Rule LP4.1 (Aadhaar Verhoeff):** Aadhaar must pass the Verhoeff check-digit algorithm (failure code: COM103). For individuals, must be exactly 12 digits. Failure rejects the submission.
- **Rule LP4.2 (PAN Caution List):** PAN status must be 'Allowed'. Any other status raises LN5337 and halts processing. A caution-list match automatically triggers an email alert to the monitoring team.
- **Rule LP4.3 (KYC Format):** PAN, Passport, and Voter ID must conform to standard formats before de-duplication is attempted.
- **Rule LP4.4 (External PAN Dedup):** For non-OEM/non-partner leads, PAN is submitted to external dedup API. COM110 → hard rejection. COM111 → warning + halt for review. COM66 → soft warning recorded.
- **Rule LP4.5 (DedupLabel Mandatory):** DedupLabel must be resolved and stored before a lead is permitted to advance to Prospect. Unresolved Conflict labels must be cleared via the Exception/Review queue.
- **Rule LP4.6 (Risk Category):** Existing customers in a restricted risk category cannot have a new leasing lead progress; error raised and submission halted.
- **Rule LP4.7 (Effectively Filed Check):** Before accepting a lead, the system checks whether an 'Effectively Filed' prospect or enquiry already exists for the same KYC identifiers. If found, the flag is set and the calling process determines the response.
- **Rule LP4.8 (Gender/Occupation Consistency):** An individual applicant whose gender is recorded as Male cannot have 'HOUSE WIFE' as their occupation. Error COM122 is raised.
- **Rule LP4.9 (PAN Mandatory for Individuals):** Individual lessees must provide a PAN unless they hold a valid PAN exemption (DAN with a valid form code and exemption unique number). If no exemption applies and PAN is absent, error COM129 is raised.
- **Rule LP4.10 (OEM/Partner Exemption List — NEW v2.0):** The following named OEM and partner sources are exempt from external PAN de-duplication checks: **MSIL (Maruti Suzuki), TMF (Toyota Motors Finance), TAFE (Tractors and Farm Equipment), HYUNDAI.** Additional OEM/partner codes may be added to this exemption list via configuration. All other sources are subject to external dedup (Rule LP4.4).
- **Rule LP4.11 (Internal KYC Dedup Error Codes — NEW v2.0):** Internal de-duplication checks against the leasing system's own records raise the following error codes:
  - **LN4084** — duplicate PAN already exists on a lead record in the system.
  - **LN4088** — duplicate PAN already exists on an application record in the system.
  - **LN3712** — duplicate KYC document (Aadhaar/Passport/Voter ID/Driving Licence) already exists across records.
  These codes are surfaced to the processing team for review but do not all result in hard rejection; routing is determined by the associated record status.
- **Rule LP4.12 (Fraud Status Codes FA/F — NEW v2.0):** Applicant records carrying fraud status code **'FA'** (fraudulent applicant confirmed) result in immediate halt and Central Team escalation. Records with status **'F'** (fraud flag raised) are routed to the fraud review queue but may continue in a restricted state pending investigation.
- **Rule LP4.13 (Full Profile on Dedup Match — NEW v2.0):** On any de-duplication match (internal or UCIC), the full customer profile must be displayed to the processing user. The profile includes: name, existing lead/prospect/application IDs, assigned branch and officer, constitution type, PAN/GSTIN (partially masked per data protection), current status, and UCIC. This requirement applies across all channels where a UI is presented.
- **Rule LP4.14 (CKYC De-duplication — NEW v2.0):** CKYC numbers, when provided, are included in the de-duplication check alongside PAN/GSTIN. A CKYC number already linked to a different customer record raises COM105 and halts processing.

---

### LP5. Lead Assignment & Re-assignment (Hierarchy)

#### i. Process Definition
Assign and re-assign lead ownership through the organisational hierarchy up to four levels with SLA tracking and escalation rules.

#### ii. Prerequisites
Lead exists and is active; users have role-based permissions.

#### iii. Process Details

**Assignment Hierarchy (up to 4 levels):**
1. **Level 1 — Central Processing Team (CPU):** Default owner for all created and uploaded leads.
2. **Level 2 — Branch Manager:** CPU assigns lead to Branch Manager.
3. **Level 3 — Field Officer / Sales Associate:** Branch Manager assigns to Field Officer.
4. **Level 4 — Call Centre Associate:** CPU or Branch Manager assigns to Call Centre for parallel follow-up.

**Re-assignment Rules:**
- Branch Manager may re-assign to another officer within the branch or return the lead to CPU.
- Field Officer may request re-assignment back to Branch Manager.
- Branch change after initial branch assignment is permitted only via an exception workflow with mandatory approval and audit trail.
- CPU may forcibly re-assign any lead at any level (configurable permission).

**Each assignment/re-assignment captures:** Assigned-to user, reason code, remarks (free text), timestamp, and previous owner — all stored in a non-modifiable audit log.

**SLA Timers:**
- SLA timer starts from the moment of each assignment.
- Configurable SLA thresholds per hierarchy level (e.g., CPU-to-Branch: 4 hours; Branch-to-Officer: 24 hours).
- SLA breach triggers escalation to the next-level manager.

**FO Absence Rule:** If the assigned Field Officer is marked as absent (leave/system flag), the lead is automatically escalated to the Branch Manager. The Branch Manager is notified via system notification.

#### iv. Touchpoints
Assignment module; Org hierarchy master; SLA engine; Notification service.

#### v. Business Rules
- **Rule LP5.1 (No Unassigned Leads):** No lead shall remain unassigned at any point after creation.
- **Rule LP5.2 (Remarks Mandatory):** Every assignment and re-assignment must capture remarks and a reason code. These are stored immutably in the audit log.
- **Rule LP5.3 (Branch Change via Exception):** Once a branch is assigned, a branch change requires an exception workflow with explicit approval and audit trail.
- **Rule LP5.4 (SLA Escalation):** SLA breaches trigger escalation to the next-level manager. All breaches are logged and reported on dashboards.
- **Rule LP5.5 (FO Absence Escalation):** If the assigned FO is absent, the lead escalates to the Branch Manager automatically.

---

### LP6. Interaction Logging & Next Action Scheduling

#### i. Process Definition
Record all interactions and follow-ups with the lead and maintain a complete, immutable interaction history. The system also generates autonomous log entries for system-triggered events.

#### ii. Prerequisites
Lead is assigned to an associate; interaction channel is available.

#### iii. Process Details

**Interaction Types Supported:** Phone call, email, WhatsApp/messaging, in-person meeting, video call (optional/nice-to-have), system-generated autonomous entry.

**For each interaction, capture:**
- Interaction type (mandatory).
- Timestamp — date and time (mandatory).
- Outcome/notes (free text, mandatory).
- Contact person at lessee side (name and designation for commercial leads).
- Mode of contact.

**Autonomous Transaction Logging (NEW v2.0):** For certain system-triggered events, the system automatically creates an interaction log entry without user action. These include:
- Automatic lead temperature degradation (Hot→Warm→Cold) — system creates an entry with type 'SYSTEM', reason 'AUTO_DEGRADATION', and the computed threshold detail.
- SLA breach and escalation events — system creates an entry recording the escalation target and SLA details.
- Caution-list match alerts — system creates an entry when a caution match is detected.
- Post-save caution screening results — system creates an entry for the caution screening outcome.
- Fraud screening results from Hunter/Sherlock — system creates entries recording each service's result code.
These autonomous entries carry operator = 'SYSTEM', are immutable, and are visible in the interaction history alongside user-created entries.

**Next Action Scheduling (mandatory for in-progress leads):**
- Date and time of next follow-up.
- Mode of next contact.
- Contact person for next interaction.
- Reminder flag (optional).

**Attempt Counters:** The system maintains counters for: number of call attempts, number of messages sent, number of emails sent. These counters feed into the Lead Temperature classification engine.

**History Immutability:** Once an interaction is logged (by a user or autonomously by the system), it cannot be deleted or modified. All interaction records carry a created-by, created-at audit stamp.

**SMS/Communication Dispatch:**
- Consent SMS may be sent to the lessee at configurable stages.
- SMS dispatch failure does not halt or roll back any transaction — failure is logged internally and suppressed from the user-facing response.

#### iv. Touchpoints
Interaction module; Email/messaging provider (optional); Calendar/reminder integration (optional); Rules engine (autonomous log trigger).

#### v. Business Rules
- **Rule LP6.1 (Mandatory Fields):** Interaction type and timestamp are mandatory for every user-created interaction record.
- **Rule LP6.2 (Next Action Mandatory):** For leads in 'In-Progress' status, scheduling a next action (date/time + mode) is mandatory before the user can exit the interaction form.
- **Rule LP6.3 (Immutable History):** Interaction history records — whether user-created or system-generated — are immutable. No deletion or editing of past interactions is permitted.
- **Rule LP6.4 (SMS Non-Blocking):** SMS dispatch failures are logged silently. They do not roll back any transaction or return an error to the user.
- **Rule LP6.5 (Attempt Counters):** Counters for call/message/email attempts are system-maintained and used as inputs to the Lead Temperature engine.
- **Rule LP6.6 (Autonomous Transaction Logging — NEW v2.0):** System-triggered events (temperature degradation, SLA breach, caution match, fraud screening result) must generate autonomous log entries with operator='SYSTEM'. These entries are non-deletable and visible in the full interaction history.

---

### LP7. Lead Qualification & Status Classification

#### i. Process Definition
Classify leads into Cold/Warm/Hot temperature buckets based on interactions, qualification outcomes, and system rules.

#### ii. Prerequisites
At least one interaction has been recorded OR qualifying data has been captured.

#### iii. Process Details

**System-Suggested Temperature:** The system evaluates:
- Attempt counters (calls, messages, emails).
- Interaction outcomes (positive/neutral/negative).
- Data completeness score (how many optional fields have been filled).
- Recency of last interaction.
- Response from lessee (positive response increases temperature; no response decreases).

**Temperature Levels:**
- **Hot** — High engagement; clear intent to lease; critical fields completed.
- **Warm** — Moderate engagement; interest expressed but not confirmed.
- **Cold** — Low or no engagement; multiple unanswered attempts.

**Manual Override:** A user may override the system-suggested temperature. Override requires:
- A mandatory Status Reason (free text + reason code).
- The StatusSuggestedBy field is set to 'Manual'.

**Degradation Rules:**
- Hot → Warm: Configurable inactivity period (e.g., 7 days without positive interaction).
- Warm → Cold: Configurable inactivity period (e.g., 14 days without response).
- Degradation is automatic with a system-generated audit entry (see LP6.6).

**Closure Triggers:** Negative outcome (e.g., lessee declined, no contact after N attempts) triggers a system suggestion to close the lead. Closure requires a mandatory ClosureReason code.

#### iv. Touchpoints
Rules engine; Dashboard module; Audit log.

#### v. Business Rules
- **Rule LP7.1 (StatusSuggestedBy):** StatusSuggestedBy is always recorded as 'System' or 'Manual'.
- **Rule LP7.2 (Override Reason):** Manual temperature overrides require a mandatory StatusReason. All status changes are auditable.
- **Rule LP7.3 (Degradation):** Degradation rules (Hot→Warm→Cold) are applied automatically based on configurable inactivity thresholds. Each degradation generates an autonomous log entry per LP6.6.
- **Rule LP7.4 (Closure Reason):** Lead closure requires a standardised ClosureReason code. Closed leads are locked from further assignment or status changes.

---

### LP8. Move to Prospect / Closure

#### i. Process Definition
Promote a qualified lead to the Prospect stage or close the lead with standardised closure reasons and an immutable audit trail.

#### ii. Prerequisites
Lead is in active (In-Progress) status. For Prospect promotion: DedupLabel is resolved (not Conflict); PAN or GSTIN is provided and validated; three-check gate is satisfied.

#### iii. Process Details

**Promote to Prospect:**

*Pre-promotion validation checklist:*
1. Lead status is In-Progress (not Closed or already Promoted).
2. DedupLabel must be resolved — Conflict labels must be cleared through the Exception/Review queue.
3. **PAN is mandatory for Individual lessees** (unless a valid PAN exemption applies).
4. **GSTIN is mandatory for Commercial/Non-Individual lessees.**
5. PAN format validated (standard alphanumeric format AAAAA9999A).
6. GSTIN format validated.
7. Aadhaar (if provided) passes the Verhoeff check-digit algorithm (COM103) and is exactly 12 digits.
8. PAN checked against caution list — status must be 'Allowed'.
9. External PAN de-duplication check for non-OEM leads (same rules as LP4.4; OEM exemption per LP4.10).
10. **Deceased lessee check:** A lessee who is marked as deceased in customer records cannot be promoted. Error message: "This Customer is deceased."
11. **Active Lease Block check:** If the system parameter for active lease blocking is enabled, check whether the lessee already holds an active lease contract that precludes new origination. Raise error LN4323 equivalent if blocking condition met.
12. **NRI eligibility check** (if applicant residential type = 'NRI'):
    - Passport number must be present.
    - Passport validity date must not be in the past and must extend at least **90 days into the future**.
    - Alternate mobile number and email address are mandatory.
    - NRI lessees are eligible only for: Individual constitution type, New Asset lease, Cars & UVs asset class, Private use — any deviation rejects the promotion.
13. Asset cost (if provided) must be greater than zero.
14. Asset cost must be greater than or equal to the requested lease/finance amount.
15. Asset model year (for asset category 1) must fall within the permitted range calculated from the current year and system parameter HP024.
16. **Location name** for each applicant record must not be null — error LN5078 if any null location name is found.
17. **Address lines** must be between 3 and 40 characters — error LN5226/LN5227 if outside range.
18. **(NEW v2.0) Three-check gate:** Before promotion, the system verifies three concurrent conditions:
    a. No open caution-list match exists unresolved for any applicant in the lead.
    b. No unresolved fraud flag (status FA or F) exists on any applicant record.
    c. The lead's source-of-business code remains active in the source-of-business master at the time of promotion (not just at creation).
    If any of the three checks fails, promotion is blocked with a specific reason code for the failing check.
19. **(NEW v2.0) One-branch restriction:** A lead may only be promoted to Prospect within the branch to which it was originally assigned or explicitly transferred via exception workflow. Attempting to promote a lead from a different branch raises error LN5098: "This lead is already being processed in another branch."

*If all validations pass:* The Prospect ID is generated (format: PR-YYYY-NNNNNN), full lineage is established (Lead → Prospect), and the lead record is updated to status 'Promoted'. The Prospect record is created in 'Draft' status awaiting PAN/GSTIN validation.

*UCIC mapping:* If the lessee is a known existing customer (DedupLabel = PossibleExisting), map the Prospect record to the existing UCIC and customer codes.

*Notification:* If the lead was created by a user other than the designated marketing/sales employee, system notifications EQ001/EQ002/EQ003 are dispatched to the affected marketing employee.

**Close Lead:**
- Record ClosureReason (standardised reason code + free text remarks).
- **Set APC/ANC codes on applicant records (NEW v2.0):** When closing a lead, the system sets the Enquiry Decision Indicator on each applicant record:
  - **APC** (Applicant Positive Closed) — used when the closure is a positive outcome (e.g., converted to another product, lessee chose another route).
  - **ANC** (Applicant Negative Closed) — used when the closure is a negative outcome (e.g., declined by lessee, not interested, deceased).
  The APC/ANC code is determined by the closure reason category. Records with ANC status trigger LN4926 and are preserved in the applicant consent chain per PP7.10.
- Lead status → Closed.
- Interaction history becomes immutable.
- No further assignment, interaction logging, or status changes are permitted on a closed lead.
- Closure date and operator are stored in the audit log.

#### iv. Touchpoints
Prospect onboarding module; External PAN/GSTIN validation service; Caution list; UCIC mapping service; Audit log; Notification service; Source-of-business master; APC/ANC code master.

#### v. Business Rules
- **Rule LP8.1 (Prospect Promotion Gate — PAN/GSTIN):** PAN is mandatory for individuals; GSTIN is mandatory for commercial lessees before Prospect promotion is allowed.
- **Rule LP8.2 (DedupLabel Resolved):** Prospect promotion is blocked if DedupLabel = Conflict. Must be resolved via exception queue.
- **Rule LP8.3 (Deceased Lessee Block):** A deceased lessee cannot be promoted to Prospect.
- **Rule LP8.4 (Active Lease Block):** If system parameter is enabled, a lessee with a blocking active lease contract cannot create a new lease lead/prospect. Error equivalent to LN4323.
- **Rule LP8.5 (NRI Eligibility):** NRI lessees must have a valid passport (not expired; valid ≥ 90 days), alternate mobile, and email. Eligible only for: Individual/New Asset/Cars & UVs/Private use.
- **Rule LP8.6 (Asset Cost Validation):** Asset cost must be > 0. Asset cost must be ≥ lease/finance amount.
- **Rule LP8.7 (Asset Model Year):** For asset category 1, model year must be within the permitted range per system parameter HP024.
- **Rule LP8.8 (Closure Reason Mandatory):** Lead closure requires a standardised ClosureReason code. Closed leads are locked.
- **Rule LP8.9 (Marketing Employee Notification):** System notifications dispatched when enquiry creator differs from the marketing employee assigned to the lead.
- **Rule LP8.10 (One-Branch Restriction — NEW v2.0):** A lead may only be promoted to Prospect within the branch to which it is assigned. An attempt to promote from a different branch raises LN5098. Branch change after assignment requires exception workflow per LP5.3.
- **Rule LP8.11 (Branch Activation Date at Promotion — NEW v2.0):** The assigned branch's activation date must be on or before the promotion date. If not, the promotion is rejected with error LN5099 equivalent.
- **Rule LP8.12 (Three-Check Gate — NEW v2.0):** Before Prospect promotion, the system verifies: (a) no unresolved caution match, (b) no fraud flag FA/F on any applicant, and (c) the source-of-business code is still active. Any failing check blocks promotion with a specific reason code.
- **Rule LP8.13 (APC/ANC Codes on Closure — NEW v2.0):** Lead closure sets APC or ANC on each applicant record based on the closure reason category. ANC records trigger LN4926 and are preserved in the consent chain. APC/ANC assignment is mandatory and stored in the audit log.

---

## 6. Process Flow — Prospect Creation

### PP1. Prospect Interface — View Confirmed Lead & History

#### i. Process Definition
Provide an interface to view Prospect records created from confirmed leads, displaying the complete lead generation history.

#### ii. Prerequisites
Lead marked as Promoted (Confirmed); user has role-based access to the Prospect module.

#### iii. Process Details
Central Team and assigned users access the Prospect interface. The interface displays:
- Lead lineage: Lead Reference Number, Temp Customer Number, Source Category/Name, creation date, all interaction history (including autonomous system entries).
- Base identifiers: Contact name, mobile, email, PAN/GSTIN (masked as per data protection policy), CKYC number (masked), Lead Type.
- Dedup status: DedupLabel, UCIC (if matched), existing customer codes, full customer profile on match.
- Lead temperature history and status change log.

For existing customers, it is permissible to start at the Prospect stage directly (skipping Lead) when policy permits, retaining the lineage link if a Lead record exists.

#### iv. Touchpoints
Prospect UI; Lead history service; Interaction log service; UCIC mapping service.

#### v. Business Rules
- **Rule PP1.1 (Confirmed Lead Requirements):** The converted lead must contain PAN or GSTIN (per policy), a contact name, and a contact number.
- **Rule PP1.2 (RBAC):** Visibility of Prospect records follows the assignment and role hierarchy. Users can only see Prospects assigned to them or within their reporting hierarchy.

---

### PP2. Prospect Assignment & Re-assignment Workflow

#### i. Process Definition
Assign Prospects through the Central Team → Manager → Associate/Call Centre hierarchy; enable re-assignment with audit trail.

#### ii. Prerequisites
Prospect record exists; branch and managerial hierarchy are available.

#### iii. Process Details
- Central Team assigns Prospect to Manager and/or Associate.
- Manager can re-assign to another associate, another branch (exception-based), or return to Central Team.
- Associate can request re-assignment back to Manager.
- All changes capture remarks, reason code, and timestamp in the immutable audit log.

**Branch Change Exception Workflow:** Once a branch is assigned to a Prospect, changing the branch requires an exception workflow with:
- Mandatory approval from Central Team or senior manager.
- Full audit trail with reason code.

#### iv. Touchpoints
Org hierarchy master; Assignment module; Audit log; Notification service.

#### v. Business Rules
- **Rule PP2.1 (No Unassigned Prospects):** No Prospect shall remain unassigned.
- **Rule PP2.2 (Remarks Mandatory):** Remarks and reason code are mandatory for each assignment/re-assignment.
- **Rule PP2.3 (Branch Change Exception):** Branch change after initial assignment requires exception approval with audit trail.

---

### PP3. PAN/GSTIN Validation & Legal Name/Address Enrichment

#### i. Process Definition
Validate PAN (individual) or GSTIN (non-individual) using an external validation service and populate legal name and registered address for the Prospect record.

#### ii. Prerequisites
Prospect creation request received; identity details (PAN or GSTIN) available; target branch is active.

#### iii. Process Details

**Individual Lessees (PAN Validation):**
1. Submit PAN to the external PAN validation service.
2. On success: auto-populate Legal Name (as per PAN records) and Registered Address.
3. Store alias/brand name (Known As) in addition to legal name.
4. Store validation response metadata: source, timestamp, validation reference.

**Commercial/Non-Individual Lessees (GSTIN Validation):**
1. Submit GSTIN to the external GST validation service.
2. On success: auto-populate Legal Entity Name and Registered Address.
3. Derive PAN from GSTIN (characters 3–12) where applicable.
4. Store alias/brand name.
5. Store validation response metadata.

**Validation Failure Handling:**
- Route Prospect to exception handling queue with mandatory reason code.
- Central Team is notified.
- Prospect remains in 'Draft' status until validation succeeds.

**Manual Override:** If a manual correction to the auto-filled legal name or address is required:
- Mandatory reason for override must be captured.
- Override is stored in audit log with operator identity and timestamp.

**Prospect ID Generation:** A unique Prospect ID (format: PR-YYYY-NNNNNN) is generated only after successful PAN/GSTIN validation. Same lock-protected sequential generation logic applies as for LRN generation.

**Geographic Validation:** The lessee's registered address (pincode) is validated against the assigned branch's geographic service area:
- The system determines the distance between the lessee's pincode and the branch. The **default maximum radius is 35 km** unless a branch-specific override is configured.
- If distance exceeds the branch's configured maximum, the system checks whether the pincode is explicitly mapped to the branch in the override table.
- Validation is bypassed for SME branches, users with designated bypass access, and transactions on the explicit exclusion list.
- Validation failure: informational warning (mobile channel) or hard error (other channels) — error LN3955.
- Geographic warning allows the user to acknowledge and proceed (save-allowed indicator = 'Y').

#### iv. Touchpoints
External PAN validation service; External GST validation service; Prospect master; Address master; Pincode-Branch mapping table.

#### v. Business Rules
- **Rule PP3.1 (Validation Gate):** Prospect ID is generated **only after** successful PAN/GSTIN validation.
- **Rule PP3.2 (Auto-fill from Validated Source):** Legal name and registered address must be populated from the validated external source — not manually entered as primary data.
- **Rule PP3.3 (Manual Override Audit):** Manual overrides of validated data require a reason code and are auditable.
- **Rule PP3.4 (Validation Failure → Exception):** Validation failure routes to exception queue with reason code; Prospect stays in Draft.
- **Rule PP3.5 (Geographic Validation):** Lessee's registered pincode is validated against branch service area. Default radius 35 km; SME branches are exempt. Hard error for non-mobile channels; warning for mobile.
- **Rule PP3.6 (Active Branch Required):** The Prospect can only be processed for a branch that is currently active — with open business and accounting dates and no close dates recorded.

---

### PP4. Meeting & Interaction Management

#### i. Process Definition
Log meetings and calls against the Prospect and provide scheduling with configurable reminders. System-generated autonomous entries are included in the Prospect interaction history.

#### ii. Prerequisites
Prospect exists and is assigned to an associate.

#### iii. Process Details
- Associate schedules a meeting against the Prospect or a specific Opportunity.
- Meeting details captured: Meeting ID (system-generated), Meeting Date & Time (mandatory), Attendees and their Designations, Notes/Remarks (mandatory), Outcome, Mode (in-person/video/phone).
- Reminders: optional calendar integration or in-application notification at configurable intervals (e.g., 1 day before, 1 hour before).
- Multiple meetings per Prospect are allowed.
- Meeting history is immutable once logged.
- System-generated autonomous entries (per LP6.6) appear in the Prospect interaction history alongside user-created entries.

#### iv. Touchpoints
Calendar/reminder integration (optional); Meeting log module; Interaction history.

#### v. Business Rules
- **Rule PP4.1 (Mandatory Fields):** Meeting Date & Time and Notes are mandatory.
- **Rule PP4.2 (RBAC):** Meeting visibility is role-based; associates see their own; managers see their team's.
- **Rule PP4.3 (Immutability):** Logged meetings cannot be deleted or edited. Amendments are logged as new entries with reference to the original.

---

### PP5. Opportunity Creation (per LoB) and Tagging

#### i. Process Definition
Create Opportunity IDs against the validated Prospect to track asset interest and LoB-specific leasing journeys.

#### ii. Prerequisites
Prospect is validated (status = Active or Validated) and active; at least one LoB interest has been identified.

#### iii. Process Details
- Associate creates an Opportunity record with mandatory fields.
- **Mandatory fields:** Asset Category, Asset Class, LoB Tag (Leasing/Lending/Deposits/Others), Contract Type.
- **Optional fields:** Asset Make, Model, Variant/Type, Colour, Quantity.
- **Contract Type Classification (NEW v2.0):** Contract type codes must be validated against the configured contract type master. The following contract type codes have special processing rules and are collectively referred to as **Vruddhi types**: **12 (Operating Lease — Short Term), 22 (Finance Lease — Balloon), 41 (Sale and Leaseback), 42 (Sub-Lease), 43 (Rental Agreement), 44 (Hire Purchase — Extended).** Vruddhi-type opportunities may trigger additional eligibility checks at the quote and application stages.
- If more than one LoB discussion occurs, a **separate Opportunity is created per LoB** — each opportunity is independent and tracked separately.
- Each Opportunity is tagged with the discussion context and contact person.
- Duplicate opportunity detection: if an Opportunity with the same Asset Category, Asset Class, and LoB Tag already exists in an active state for the Prospect, the system warns the user (configurable: warn or block).

**Opportunity ID Generation:** Unique Opportunity ID (format: OPP-YYYY-NNNNNN) generated at creation.

#### iv. Touchpoints
Opportunity module; Asset taxonomy master (Category/Class/Make/Model); Contract type master.

#### v. Business Rules
- **Rule PP5.1 (Mandatory Asset Fields):** Asset Category and Asset Class are mandatory at Opportunity creation.
- **Rule PP5.2 (LoB Tag Mandatory):** Each Opportunity must carry a LoB tag.
- **Rule PP5.3 (Multi-LoB):** Multiple Opportunities per Prospect are allowed; each is managed independently.
- **Rule PP5.4 (Duplicate Control):** Duplicate Opportunity rules (same asset + LoB) are configurable as warn or block.
- **Rule PP5.5 (Contract Type Validation — NEW v2.0):** Contract type must be a valid code in the contract type master. Vruddhi types (12/22/41/42/43/44) are flagged and subject to additional eligibility checks at subsequent stages.

---

### PP6. Quote Generation (Rack Rate / Customised) & Negotiation

#### i. Process Definition
Generate standard (rack rate) quotes and allow customised quotes during negotiation; support multiple quotes per Opportunity with version tracking and locking.

#### ii. Prerequisites
Opportunity exists and is active; asset details are sufficient for pricing; user has quote permission.

#### iii. Process Details

**Product Model Market Value Retrieval:** Before generating a quote, the system retrieves the current dealer/market price (NDLP equivalent) for the product model based on: contract/lease type, asset class, dealer code, lessee location, product model, and quantity.
- If the system price differs from the submitted asset cost, the asset cost is replaced with the system NDLP price.
- An advisory message is returned: "Asset Cost has been updated to the current market price."

**Rack Rate Quote:** Associate fetches the standard lease rate from the Quote Master (based on asset taxonomy, lease type, and standard pricing slabs) and attaches it to the Opportunity. Rack rate quotes do not require additional approval.

**Customised Quote:** For deviations from rack rate:
- Associate captures all required fields: Asset Category, Asset Class, Make, Model, and pricing inputs (GST rate, depreciation, corporate tax, cost of funds, residual value, etc. — as required by the pricing engine).
- Customised quotes that deviate from rack rate require an **approval workflow** (Manager or Central Team approval).
- Approval decisions are auditable.

**Multiple Quotes per Opportunity:** Multiple quote versions may be attached to a single Opportunity. Negotiation history is maintained across versions.

**Quote Lock:** On customer/lessee confirmation:
- The final quote is locked (Quote Status → Locked).
- Post-lock edits are blocked unless an explicit unlock approval is obtained from the Manager or Central Team.
- Approval for post-lock re-negotiation is auditable.

**Vruddhi Contract Type Quoting (NEW v2.0):** For Opportunities with Vruddhi contract types (12/22/41/42/43/44):
- Additional eligibility parameters are validated before the quote is saved (e.g., sub-lease counterparty details for type 42, short-term duration limits for type 12).
- The Quote Master must contain valid rates for the Vruddhi type; if rates are absent, the user is prompted to request customised pricing.

#### iv. Touchpoints
Quote Master; Pricing engine (optional); Document attachment service; Email/notification service; Product model price retrieval service.

#### v. Business Rules
- **Rule PP6.1 (Quote Tagged to Opportunity):** Every quote must be tagged to a valid Opportunity ID.
- **Rule PP6.2 (Asset Cost > 0):** Asset cost must be greater than zero. Error equivalent to LN3574 if violated.
- **Rule PP6.3 (Asset Cost ≥ Lease Amount):** Asset cost must be ≥ requested lease/finance amount.
- **Rule PP6.4 (Market Value Override):** If the retrieved market value differs from the submitted asset cost, the market value replaces the submitted value and an advisory message is returned.
- **Rule PP6.5 (Customised Quote Approval):** Deviations from rack rate require manager/central team approval. Unapproved deviations cannot be shared with the lessee.
- **Rule PP6.6 (Quote Lock Enforcement):** A locked quote cannot be edited without an approved unlock. Lock state and all unlock approvals are auditable.
- **Rule PP6.7 (Appraisal Category Validation):** The selected appraisal/assessment category is validated against branch, lease type, asset class, market value, and the creation date. Invalid combinations are rejected.
- **Rule PP6.8 (Vruddhi Type Quoting — NEW v2.0):** Opportunities with Vruddhi contract types (12/22/41/42/43/44) require additional validation before a quote can be saved. If the Quote Master lacks rates for the type, customised pricing must be requested.

---

### PP7. Application Origination from Prospect (Parallel KYC & CAM)

#### i. Process Definition
Enable lease application processing to start from the Prospect stage; KYC and Credit Appraisal (CAM) run in parallel, with statuses visible at the Prospect level.

#### ii. Prerequisites
Prospect is active (status = Active); at least one Opportunity and/or locked Quote exists; user has origination permission.

#### iii. Process Details

**Application Initiation:**
- Associate initiates application processing from the Prospect screen.
- Application ID generated (format: APP-YYYY-NNNNNN).
- Form-fill automation populates fields from Prospect data where available.
- Required documents checklist is enforced by stage (configurable per asset class and lease type).

**KYC Initiation:** KYC workflow is initiated from the Prospect stage. KYC documents are uploaded — a compressed archive of KYC images is accepted:
- Archive is saved to a configured temporary directory under a composite identifier (Application Number + timestamp).
- Each document is extracted and classified by type: Photo, Driving Licence, PAN, Passport, or other KYC type.
- Document type names are normalised: 'APPLICANT' → 'PHOTO'; 'LICENCE' → 'DRIVING LICENCE'.
- Applicant type is derived from the filename prefix: 'MA' → 'MAIN APPLICANT'; 'A1' → 'ADDL APPLICANT - 1'; etc.
- KYC images are transmitted to the Document Management System (DMS) via environment-appropriate endpoint: 'T' → Test; 'B' → Beta; 'L' → Live (Production).
- Each successful upload returns a document index.

**CAM Initiation:** Credit Appraisal (CAM) is initiated in parallel with KYC — both processes run concurrently without blocking each other.

**Status Tracking:** The Prospect screen displays:
- Application ID and status (Initiated / In Review / Approved / Rejected).
- KYC status (Not Started / In Progress / Complete).
- CAM status (Not Started / In Progress / Approved / Declined).
- Sanction ID and sanction package (once issued by Central Team).

**Fraud Screening & Credit Bureau:**
- After successful application creation, the system checks whether the online fraud screening service is active.
- If active, Hunter and Sherlock fraud detection services are invoked.
  - If both return clear results: fraud status = Clear; credit bureau (CIBIL) request is initiated.
  - If either returns a non-clear result: application remains in Pending status; fraud status message is recorded. **Applicant records with Hunter result 'FA' or Sherlock result 'F' are flagged as fraud-blocked (see LP4.12).**
- If the online fraud service is not active: credit bureau service is called directly for individual lessees.
- Credit bureau service makes an HTTP GET call; 2xx response = success.
- Fraud screening failures do not block the overall transaction — the application is saved with Pending status.
- **Autonomous log entries are created for each fraud screening result per LP6.6.**

**CIBIL Re-initiation Control (NEW v2.0):** CIBIL requests are subject to the following re-initiation rules:
- **LN3786:** A CIBIL request may only be re-initiated if the previous request returned a non-2xx response or a system error. Successful CIBIL responses are not eligible for re-initiation unless the configured re-initiation window (system parameter) has elapsed.
- **LN3780:** The system tracks the number of CIBIL re-initiation attempts per application. Attempts exceeding the configured maximum (system parameter) are blocked and require Central Team approval.
- **LN3782:** If a CIBIL request is re-initiated within the same processing day as a previous successful request, the system raises an informational warning; a second same-day request requires explicit user confirmation.

**Existing Customer Fast-Track:** For existing customers within a valid sanction period, only limit checking may be required (substituting full CAM workflow — policy-controlled and configurable).

**Modification Controls:**
- Modification of an application is blocked if a lease contract or CAM application already exists in a non-final state (error equivalent to LN3713).
- Modification is blocked if the application is under active fraud investigation (error equivalent to LN3785).

**Two-Stage Branch Email (NEW v2.0):** For branches operating a two-stage approval process, upon successful application creation, the system dispatches an automated email to the Branch Head (or configured notification recipient) containing the application summary, applicant name, application ID, and the next-step instructions. Email dispatch failure is logged silently per LP6.4.

#### iv. Touchpoints
Origination module; Document Management System; CAM workflow; Fraud detection services (Hunter/Sherlock); Credit bureau service (CIBIL); Status reporting layer; Email notification service.

#### v. Business Rules
- **Rule PP7.1 (Status Visible at Prospect):** The Prospect record must display Application, KYC, and CAM statuses at all times.
- **Rule PP7.2 (Document Checklist Enforced):** Required documents checklist is enforced by stage/asset class/lease type. Missing documents block stage progression.
- **Rule PP7.3 (DMS Environment Routing):** KYC document images are routed to Test, Beta, or Live DMS endpoint based on the environment indicator in the system parameter registry.
- **Rule PP7.4 (Fraud Screening Non-Blocking):** Fraud screening outcome does not block the application save. Non-clear results set the application to Pending status for manual review.
- **Rule PP7.5 (Modification Block — Contract In Progress):** Modification blocked if lease contract or CAM in a non-final state.
- **Rule PP7.6 (Modification Block — Fraud Investigation):** Modification blocked if application is under active fraud investigation.
- **Rule PP7.7 (Credit Bureau Call):** Credit bureau (CIBIL) request is initiated for individual lessees upon fraud clearance or directly if fraud screening is inactive. Non-2xx credit bureau response is logged but does not block the transaction.
- **Rule PP7.8 (Sanction Validity — Existing Customer):** Sanction validity period is configurable. Within validity, existing customers may proceed via limit checking only, bypassing full CAM.
- **Rule PP7.9 (Non-Individual Lessee Auto-Eligibility):** When the main lessee is a Non-Individual (commercial entity) and the branch operates a single-stage approval process, the application is automatically advanced to Eligible for Application status upon creation — without waiting for fraud check results. This rule does not apply to two-stage approval branches.
- **Rule PP7.10 (Modification Delete-and-Re-insert with Consent Preservation):** During application modification, all applicant detail records are deleted and re-inserted with the updated data, except applicants who have already received a confirmation SMS or whose applicant status is 'ANC'. These records are preserved in order to protect the consent chain and audit trail.
- **Rule PP7.11 (Post-Save Caution List Screening):** After the application and all applicant records are committed to the database, all applicants are screened against the internal caution database. If no hard error is returned, a second commit is issued. A hard error at this stage blocks the second commit and the result is logged for Central Team review. **An autonomous log entry is created for the screening result per LP6.6.**
- **Rule PP7.12 (Welcome Communication for Eligible Lease Application):** When a new lease application of a designated type becomes immediately eligible for application status (e.g., equivalent to Hire Purchase / Finance Lease on immediate EA), a welcome communication (email and/or SMS) containing the application details and next-steps link is dispatched to the lessee. The specific eligible lease types are configurable in the system parameter registry.
- **Rule PP7.13 (Two-Stage Branch Email — NEW v2.0):** For branches with two-stage approval configuration, an automated email notification is dispatched to the Branch Head upon successful application creation. The email contains application summary, applicant name, application ID, and next-step instructions. Email failure is logged silently.
- **Rule PP7.14 (CIBIL Re-initiation Control — NEW v2.0):** CIBIL re-initiation is controlled by three rules:
  - LN3786: Re-initiation permitted only after non-2xx response or system error on previous request, or after configured re-initiation window has elapsed.
  - LN3780: Re-initiation attempts are capped at the system-configured maximum per application; additional attempts require Central Team approval.
  - LN3782: Same-day re-initiation after a successful CIBIL response requires explicit user confirmation. An informational warning is raised.
- **Rule PP7.15 (Autonomous Logging at Application Stage — NEW v2.0):** Fraud screening results (Hunter/Sherlock), post-save caution screening results, CIBIL request status, and two-stage branch email dispatch status are all logged as autonomous entries per LP6.6. These entries appear in the Prospect and Application interaction histories.

---

### PP8. Customer Creation & Lineage

#### i. Process Definition
Create the enterprise customer record only after KYC completion and CAM approval; maintain full cross-reference lineage and trigger downstream role assignment.

#### ii. Prerequisites
KYC Status = Complete **and** CAM Status = Approved; Final quote locked (if applicable); all checklist items completed.

#### iii. Process Details

**Customer Creation Gate:** The system enforces a hard gate — enterprise customer creation is only permitted when:
1. KYC Status = Complete.
2. CAM Status = Approved.
3. All mandatory checklist items are complete (policy-based).

**Enterprise Customer ID Generation:** Upon gate satisfaction, the system generates an Enterprise Customer ID (format: CUST-YYYY-NNNNNN) and creates the customer master record.

**Full Lineage Preservation:** The system establishes and stores immutable cross-reference links:
- Lead Reference Number → Prospect ID → Opportunity ID → Quote ID → Application ID → Enterprise Customer ID.
- These lineage links are mandatory and cannot be modified once the customer record is created.

**APC Code on Customer Creation (NEW v2.0):** Upon successful customer creation, the applicant status code on all associated applicant records is updated to **APC** (Applicant Positive Closed), confirming a positive outcome for the lead-to-customer journey. This update is recorded in the audit log.

**Role Assignment:** Post customer creation, the system assigns roles per policy:
- Lessee (Individual or Corporate)
- Dealer/Vendor (if applicable)
- Depositor (if applicable)
- Role assignment requires completion of role-specific checklists.

**Downstream Triggers:** Customer creation triggers downstream processes:
- Lease Agreement/Contract generation.
- Billing and payment setup.
- Asset delivery coordination.

**Caution Email Alert on Customer Creation (NEW v2.0):** If any applicant record on the application was flagged against the caution list at any prior stage (regardless of whether it was resolved as 'Allowed'), the system generates a caution alert email to the designated monitoring team at the point of customer creation. This is a final confirmation alert — it supplements the alert raised at LP4 and PP7.11.

#### iv. Touchpoints
Customer master; Cross-reference/lineage service; Role assignment module; Downstream contract and billing systems; Email alert service.

#### v. Business Rules
- **Rule PP8.1 (Customer Creation Gate):** Enterprise Customer ID is created **only when** KYC = Complete and CAM = Approved. Both conditions must be simultaneously satisfied.
- **Rule PP8.2 (Lineage Immutability):** All lineage links (Lead → Prospect → Opportunity → Quote → Application → Customer) are mandatory and immutable once the customer record is created.
- **Rule PP8.3 (Role Assignment Checklist):** Role assignment requires completion of the policy-defined checklist for each role.
- **Rule PP8.4 (APC Code on Customer Creation — NEW v2.0):** All applicant records associated with the created customer are updated to APC status, confirming the positive closure of the lead-to-customer journey.
- **Rule PP8.5 (Caution Alert on Customer Creation — NEW v2.0):** If any applicant was previously flagged against the caution list during the lead-to-customer journey, a final caution alert email is dispatched to the monitoring team upon customer creation. Dispatch failure is logged silently.
- **Rule PP8.6 (Vruddhi Contract Type Downstream — NEW v2.0):** For customers created from applications with Vruddhi contract types (12/22/41/42/43/44), the downstream contract generation system must be notified of the specific contract type code so that the appropriate contract template and terms are applied.

---

## 7. Business Rules — Consolidated Reference

| Rule ID | Category | Statement | Process Step(s) | Enforcement Layer |
| --- | --- | --- | --- | --- |
| LP1.1 | Authentication | Every request must be authenticated before any processing. Authentication failure immediately rejects submission — no data is written. | LP1 | Both |
| LP1.2 | Authentication | Secondary device identifier substituted if primary is absent. Both absent: proceeds in legacy compatibility mode (configurable). | LP1 | Both |
| LP1.3 | Authentication | Authenticated user must have an active employee record. If absent, error GL461 (Invalid User) is raised and all processing halts. | LP1 | Processing layer |
| LP1.4 | GPS Capture | GPS coordinates captured and stored when GPS-enabled indicator is active. GPS failure does not block submission. | LP1 | Entry layer |
| LP2.1 | Input Validation | Lead input payload is mandatory. Rejection message: "Lead Input Values Is Required." | LP2 | Both |
| LP2.2 | Input Validation | Name + at least one identifier (Mobile/Email/PAN/GSTIN/Address) mandatory. Failure → Exception Queue, not immediate rejection. | LP2 | Both |
| LP2.3 | Number Generation | LRN generated by lock-protected atomic increment of central control record. Lock timeout (15 s) → error D283. | LP2 | Both |
| LP2.4 | Assignment | All leads default to CPU unless self-assign is explicitly selected (role-based permission). No unassigned leads permitted. | LP2, LP5 | Both |
| LP2.5 | Data Normalisation | Gender normalised: Male→M; others→F. Non-individual constitution type: gender field cleared. | LP2 | Entry layer |
| LP2.6 | DAN Validation | PAN exemption flag = 'Y' if DAN provided; 'N' otherwise. DAN must exist in system and must not have been previously utilised. Errors LN4468 / LN4470. | LP2 | Processing layer |
| LP2.7 | Data Normalisation | Additional co-lessees sequentially labelled: MAIN APPLICANT, ADDL APPLICANT - 1, ADDL APPLICANT - 2, etc. Main lessee always labelled MAIN APPLICANT. | LP2 | Entry layer |
| LP2.8 | Geography | If pincode provided at lead creation, validated against branch service area (default radius 35 km). Mobile: warning LN3955 (save-allowed = 'Y'). Other channels: hard error LN3955. SME branches, bypass-right users, and exclusion-list leads are exempt. | LP2 | Both |
| LP2.9 | Mobile Validation | Mobile numbers validated for country code (COM112), format (COM113), numeric-only (COM114), and minimum length (COM115). Applies to primary and alternate mobile fields. | LP2 | Entry layer |
| LP2.10 | Source Restriction | Submitting user's source-of-business code must be registered and active for the target branch. Unregistered/unauthorised source rejects submission. | LP2, LP3 | Both |
| LP2.11 | CKYC | CKYC number, if provided, must be unique — not already linked to another customer. Error COM105 on uniqueness failure. | LP2 | Processing layer |
| LP2.12 | Branch Activation | Target branch must have an activation date on or before the processing date. Future activation date raises LN5099 equivalent. | LP2 | Processing layer |
| LP2.13 | Digital Channel | Lease/contract type for digital channel origination must be in the configured digital-permitted set. Non-permitted type raises Rule 54 equivalent error. | LP2 | Both |
| LP3.1 | Bulk Upload | Upload file / API payload must contain Name + one identifier + Source Category. Invalid records → Exception Queue; not a global batch rejection. | LP3 | Both |
| LP3.2 | Assignment | All leads ingested via bulk upload or API default to CPU. No unassigned leads after ingestion. | LP3 | Both |
| LP3.3 | Number Generation | LRN generation for bulk/API records follows the same lock-protected sequential logic as LP2.3. | LP3 | Both |
| LP3.4 | Exception Queue | Exception Queue entries carry mandatory reason codes, are owned by CPU, and have configurable cure SLAs. | LP3 | Both |
| LP4.1 | KYC | Aadhaar must pass Verhoeff check-digit algorithm (failure: COM103). Individual lessees: must be exactly 12 digits. Failure rejects submission. | LP4, LP8 | Both |
| LP4.2 | KYC | PAN status must be 'Allowed' in internal caution/block list. Any other status raises LN5337 and halts processing. Caution match triggers email alert to monitoring team. | LP4, LP8 | Both |
| LP4.3 | KYC | PAN, Passport, and Voter ID must conform to standard formats before de-duplication is attempted. | LP4 | Processing layer |
| LP4.4 | Deduplication | PAN submitted to external dedup API for non-OEM/non-partner leads. COM110 → hard rejection. COM111 → warning + halt for review. COM66 → soft warning recorded; processing continues. | LP4, LP8 | Both |
| LP4.5 | Deduplication | DedupLabel must be resolved and stored before a lead may advance to Prospect. Conflict label must be cleared via Exception/Review queue. | LP4, LP8 | Both |
| LP4.6 | Eligibility | Existing customers in a restricted risk category are blocked from creating a new leasing lead. Error raised and processing halted. | LP4 | Processing layer |
| LP4.7 | Deduplication | Before accepting a lead, system checks whether an 'Effectively Filed' prospect or enquiry already exists for the same KYC identifiers. If found, flag is set for calling process determination. | LP4 | Processing layer |
| LP4.8 | Data Consistency | An individual applicant whose gender is Male cannot have 'HOUSE WIFE' as their occupation. Error COM122. | LP4 | Processing layer |
| LP4.9 | KYC | PAN is mandatory for individual lessees without a valid DAN exemption (form code + exemption unique number). If absent and no exemption, error COM129. | LP4, LP8 | Processing layer |
| LP4.10 | OEM Exemption | Named OEM/partner sources exempt from external PAN dedup: MSIL, TMF, TAFE, HYUNDAI. Additional codes configurable. | LP4, LP8 | Processing layer |
| LP4.11 | KYC Dedup Codes | Internal dedup errors: LN4084 (duplicate PAN on lead), LN4088 (duplicate PAN on application), LN3712 (duplicate KYC document). These raise alerts; routing determined by associated record status. | LP4 | Processing layer |
| LP4.12 | Fraud Status | Applicant fraud status 'FA' → immediate halt and Central Team escalation. Status 'F' → fraud review queue; restricted processing. | LP4, PP7 | Both |
| LP4.13 | Dedup Profile | On any de-duplication match (internal or UCIC), full customer profile displayed to processing user including name, IDs, branch, officer, constitution type, PAN/GSTIN (masked), status, and UCIC. | LP4 | Entry layer |
| LP4.14 | CKYC Dedup | CKYC numbers included in de-duplication set. CKYC already linked to another record raises COM105. | LP4 | Processing layer |
| LP5.1 | Assignment | No lead shall remain unassigned at any point after creation. | LP5 | Both |
| LP5.2 | Assignment | Every assignment and re-assignment must capture remarks and a reason code; stored immutably in the audit log. | LP5 | Both |
| LP5.3 | Assignment | Once a branch is assigned to a lead, a branch change requires an exception workflow with explicit approval and audit trail. | LP5 | Both |
| LP5.4 | SLA | SLA breaches trigger automatic escalation to the next-level manager. All breaches are logged and reported on dashboards. Escalation generates an autonomous log entry. | LP5 | Both |
| LP5.5 | SLA | If the assigned Field Officer is marked as absent, the lead escalates to the Branch Manager automatically; Branch Manager is notified. | LP5 | Both |
| LP6.1 | Interaction | Interaction type and timestamp are mandatory for every user-created interaction record. | LP6 | Both |
| LP6.2 | Interaction | For leads in 'In-Progress' status, next action scheduling (date/time + mode) is mandatory before the user can exit the interaction form. | LP6 | Both |
| LP6.3 | Interaction | Interaction history records (user-created and system-generated) are immutable. No deletion or editing permitted. | LP6 | Both |
| LP6.4 | Notification | SMS dispatch failures are logged silently. They do not roll back any transaction or surface an error to the user. | LP6, LP8 | Both |
| LP6.5 | Interaction | Call/message/email attempt counters are system-maintained per lead and used as inputs to the Lead Temperature classification engine. | LP6 | Both |
| LP6.6 | Autonomous Logging | System-triggered events (temperature degradation, SLA breach, caution match, fraud screening result, CIBIL status) generate autonomous interaction log entries with operator='SYSTEM'. Non-deletable; visible in full interaction history. | LP6, PP4, PP7 | Both |
| LP7.1 | Status | StatusSuggestedBy is always recorded as 'System' or 'Manual' for every temperature change. | LP7 | Both |
| LP7.2 | Status | Manual temperature override requires a mandatory StatusReason (free text + reason code). All status changes are auditable. | LP7 | Both |
| LP7.3 | Status | Temperature degradation (Hot→Warm→Cold) is applied automatically based on configurable inactivity thresholds. Each automated degradation creates an autonomous log entry per LP6.6. | LP7 | Both |
| LP7.4 | Status | Negative outcome or N unanswered attempts trigger a system suggestion to close the lead. Closure requires a standardised ClosureReason code; closed leads are locked from further updates. | LP7, LP8 | Both |
| LP8.1 | Prospect Promotion | PAN mandatory for individual lessees; GSTIN mandatory for commercial lessees before Prospect promotion is allowed. | LP8 | Both |
| LP8.2 | Prospect Promotion | Prospect promotion is blocked if DedupLabel = Conflict. Must be resolved via Exception/Review queue before promotion. | LP8 | Both |
| LP8.3 | Eligibility | A deceased lessee cannot be promoted to Prospect. Error LN3574: "This Customer is deceased." | LP8 | Both |
| LP8.4 | Eligibility | If the Active Lease Block system parameter is enabled, a lessee with a qualifying active lease contract cannot initiate a new leasing lead/prospect. Error LN4323 equivalent. | LP8 | Both |
| LP8.5 | NRI | NRI lessees must have a valid passport (not expired; valid ≥ 90 days), alternate mobile, and email. Eligible only for: Individual / New Asset / Cars & UVs / Private use. Any deviation rejects the submission. | LP8 | Both |
| LP8.6 | Asset | Asset cost must be > 0 (error LN3574). Asset cost must be ≥ requested lease/finance amount. | LP8, PP6 | Both |
| LP8.7 | Asset | Asset model year must fall within the permitted range calculated from the current year and system parameter HP024 (applies to asset category 1). | LP8 | Processing layer |
| LP8.8 | Closure | Lead closure requires a standardised ClosureReason code. Closed leads are locked from further assignment, status change, or interaction logging. | LP8 | Both |
| LP8.9 | Notification | System notifications EQ001/EQ002/EQ003 dispatched when the lead creator differs from the assigned marketing/sales employee, or when the marketing employee changes during modification. | LP8 | Processing layer |
| LP8.10 | One-Branch | Lead may only be promoted to Prospect within its assigned branch. Cross-branch promotion raises LN5098. Branch change requires LP5.3 exception workflow. | LP8 | Both |
| LP8.11 | Branch Activation | Branch activation date must be on or before promotion date. Future activation date raises LN5099 equivalent. | LP8 | Processing layer |
| LP8.12 | Three-Check Gate | Before Prospect promotion: (a) no unresolved caution match; (b) no fraud flag FA/F on applicant; (c) source-of-business still active. Any failing check blocks promotion with specific code. | LP8 | Both |
| LP8.13 | APC/ANC Codes | Lead closure sets APC (positive) or ANC (negative) on each applicant record per closure reason category. ANC triggers LN4926. APC/ANC assignment mandatory and auditable. | LP8 | Both |
| PP1.1 | Prospect View | Confirmed lead converted to Prospect must contain PAN or GSTIN, a contact name, and a contact number. | PP1 | Both |
| PP1.2 | RBAC | Prospect record visibility follows assignment and role hierarchy. Users see only Prospects assigned to them or within their reporting tree. | PP1 | Both |
| PP2.1 | Assignment | No Prospect shall remain unassigned at any point after creation. | PP2 | Both |
| PP2.2 | Assignment | Remarks and reason code are mandatory for each Prospect assignment/re-assignment; stored in immutable audit log. | PP2 | Both |
| PP2.3 | Assignment | Prospect branch change after initial assignment requires exception workflow with mandatory Central Team / senior manager approval and full audit trail. | PP2 | Both |
| PP3.1 | Validation Gate | Prospect ID is generated only after successful PAN/GSTIN validation by the external service. | PP3 | Both |
| PP3.2 | Data Integrity | Legal name and registered address must be populated from the validated external source, not manually entered as primary data. | PP3 | Both |
| PP3.3 | Override Audit | Manual overrides of validated legal name or address require a reason code and are stored with operator identity and timestamp. | PP3 | Both |
| PP3.4 | Validation Failure | PAN/GSTIN validation failure routes the Prospect to the exception queue with a mandatory reason code. Prospect remains in Draft status until validation succeeds. | PP3 | Both |
| PP3.5 | Geography | Lessee's registered pincode validated against branch service area (default radius 35 km). SME branches exempt. Hard error for non-mobile channels; warning for mobile. Error LN3955. | PP3 | Both |
| PP3.6 | Branch | Prospect can only be processed for an active branch (open business and accounting dates, no close dates recorded). Error LN480. | PP3 | Processing layer |
| PP4.1 | Meeting | Meeting Date & Time and Notes/Remarks are mandatory for every logged meeting record. | PP4 | Both |
| PP4.2 | Meeting | Meeting record visibility is role-based: associates see their own; managers see their team's. | PP4 | Both |
| PP4.3 | Meeting | Logged meetings are immutable. Amendments are recorded as new entries with a reference to the original meeting ID. | PP4 | Both |
| PP5.1 | Opportunity | Asset Category and Asset Class are mandatory at Opportunity creation. | PP5 | Both |
| PP5.2 | Opportunity | Each Opportunity must carry a LoB tag (Leasing/Lending/Deposits/Others). | PP5 | Both |
| PP5.3 | Opportunity | Multiple Opportunities per Prospect are allowed; each is managed and tracked independently. | PP5 | Both |
| PP5.4 | Opportunity | Duplicate Opportunity detection (same Asset Category + Asset Class + LoB for the same Prospect) is configurable as warn or block. | PP5 | Both |
| PP5.5 | Contract Type | Contract type must be valid per contract type master. Vruddhi types (12/22/41/42/43/44) flagged for additional eligibility checks at quote and application stages. | PP5, PP6, PP7 | Both |
| PP6.1 | Quote | Every quote must be tagged to a valid Opportunity ID before it can be saved or shared with the lessee. | PP6 | Both |
| PP6.2 | Asset | Asset cost must be > 0. Error LN3574 equivalent if violated. | PP6 | Both |
| PP6.3 | Asset | Asset cost must be ≥ requested lease/finance amount. | PP6 | Both |
| PP6.4 | Pricing | System NDLP market value overrides the submitted asset cost when they differ. Advisory message returned to caller. | PP6 | Both |
| PP6.5 | Quote | Customised quotes deviating from rack rate require Manager or Central Team approval before being shared. Unapproved deviations cannot be shared with the lessee. | PP6 | Both |
| PP6.6 | Quote | A locked quote cannot be edited without an approved unlock from Manager or Central Team. Lock state and all unlock approvals are auditable. | PP6 | Both |
| PP6.7 | Quote | Appraisal/assessment category must be valid for the combination of branch, lease type, asset class, market value, and date. Invalid combinations are rejected. | PP6 | Both |
| PP6.8 | Vruddhi Quote | Vruddhi contract types (12/22/41/42/43/44) require additional validation before quote save; absent rates prompt customised pricing request. | PP6 | Both |
| PP7.1 | Application | The Prospect record must display Application ID, KYC status, and CAM status at all times after application initiation. | PP7 | Both |
| PP7.2 | Application | Required document checklist is enforced by stage, asset class, and lease type. Missing mandatory documents block stage progression. | PP7 | Both |
| PP7.3 | Document | KYC document images are routed to the Test ('T'), Beta ('B'), or Live ('L') DMS endpoint based on the environment indicator in the system parameter registry. | PP7 | Entry layer |
| PP7.4 | Fraud | Fraud screening outcome does not block the application save. Non-clear results set the application to Pending status for manual review. | PP7 | Both |
| PP7.5 | Modification | Application modification is blocked if a lease contract or CAM application already exists in a non-final state. Error LN3713 equivalent. | PP7 | Both |
| PP7.6 | Modification | Application modification is blocked if the application is under active fraud investigation. Error LN3785 equivalent. | PP7 | Both |
| PP7.7 | Credit Bureau | Credit bureau (CIBIL) request initiated for individual lessees upon fraud clearance, or directly if fraud screening service is inactive. Non-2xx HTTP response is logged; transaction not blocked. | PP7 | Both |
| PP7.8 | Sanction | Sanction validity period is configurable. Existing customers within a valid sanction period may proceed via limit checking only, bypassing full CAM workflow. | PP7 | Both |
| PP7.9 | Eligibility | Non-individual (commercial entity) lessee at a single-stage approval branch: application automatically advances to Eligible for Application status upon creation without awaiting fraud check results. Does not apply to two-stage approval branches. | PP7 | Processing layer |
| PP7.10 | Modification | During application modification, applicant records with a confirmed SMS dispatch or ANC status are preserved to protect the consent chain. All other applicant records are deleted and re-inserted with updated data. | PP7 | Processing layer |
| PP7.11 | Post-Save | After the application and all applicant records are committed, all applicants are screened against the internal caution database. If no hard error is returned, a second commit is issued. A hard error blocks the second commit and is logged for Central Team review. Autonomous log entry created per LP6.6. | PP7 | Processing layer |
| PP7.12 | Notification | When a new lease application of a designated type becomes immediately eligible for application status upon creation, a welcome communication (email/SMS) is dispatched to the lessee. The eligible lease types are configurable in the system parameter registry. | PP7 | Processing layer |
| PP7.13 | Two-Stage Email | For two-stage approval branches, automated email dispatched to Branch Head upon successful application creation. Email dispatch failure logged silently. | PP7 | Processing layer |
| PP7.14 | CIBIL Re-initiation | LN3786: re-initiation only after failure or elapsed window. LN3780: attempt cap enforced; excess requires Central Team approval. LN3782: same-day re-initiation requires explicit user confirmation. | PP7 | Both |
| PP7.15 | Autonomous Logging | Fraud screening results, post-save caution results, CIBIL status, and email dispatch status are logged as autonomous entries per LP6.6 at the application stage. | PP7 | Both |
| PP8.1 | Customer Creation | Enterprise Customer ID is created only when KYC = Complete AND CAM = Approved. Both conditions must be simultaneously satisfied. | PP8 | Both |
| PP8.2 | Lineage | All lineage links (Lead→Prospect→Opportunity→Quote→Application→Customer) are mandatory and immutable once the customer record is created. | PP8 | Both |
| PP8.3 | Role Assignment | Role assignment (Lessee/Dealer/Vendor/Depositor) requires completion of the policy-defined checklist for each role before activation. | PP8 | Both |
| PP8.4 | APC on Creation | All applicant records updated to APC status upon successful customer creation; update auditable. | PP8 | Both |
| PP8.5 | Caution Alert | If any applicant was flagged on caution list during journey, a final alert email dispatched to monitoring team on customer creation. Dispatch failure logged silently. | PP8 | Processing layer |
| PP8.6 | Vruddhi Downstream | Downstream contract generation system notified of Vruddhi contract type codes (12/22/41/42/43/44) for correct template and terms application. | PP8 | Processing layer |

---

## 8. Validation Rules

### 8.1 Mandatory Field Validation

| Field | Stage | Condition | Error |
| --- | --- | --- | --- |
| Lead Input Payload | Lead Creation | Always | "Lead Input Values Is Required." |
| Lease/Contract Type | Lead Creation | When asset details provided | "CONTRACT_TYPE Lead Input Values Is Required." |
| Name | Lead Creation | Always | Minimum identifier rule violation → Exception Queue |
| One of: Mobile / Email / PAN / GSTIN / Address | Lead Creation | Always | Minimum identifier rule violation → Exception Queue |
| Source Category | Lead Creation | Always | Exception Queue |
| Source Name | Lead Creation | Always | Exception Queue |
| Lead Type (Individual / Commercial) | Lead Creation | Always | Mandatory |
| Company (Known As) + Contact Person | Lead Creation | If Lead Type = Commercial | Mandatory |
| Source-of-Business Code | Lead Creation | Always | Unregistered/unauthorised → rejection per LP2.10 |
| DedupLabel | Pre-Prospect Promotion | Always | Must be resolved; Conflict blocks promotion |
| PAN | Prospect Promotion | If constitution = Individual | Error COM129 |
| GSTIN | Prospect Promotion | If constitution = Commercial | Mandatory |
| Passport Number + Validity Date | Prospect Promotion | If residential type = NRI | Mandatory |
| Alternate Mobile + Email | Prospect Promotion | If residential type = NRI | Mandatory |
| Location Name | Every Applicant Record | Always | Error LN5078 |
| Address Lines | Applicant Record | Always | Must be 3–40 characters; errors LN5226/LN5227 |
| Asset Category + Asset Class | Opportunity Creation | Always | Mandatory |
| LoB Tag | Opportunity Creation | Always | Mandatory |
| Contract Type | Opportunity Creation | Always | Must be valid in contract type master |
| Meeting Date & Time + Notes | Meeting Logging | Always | Mandatory |
| Interaction Type + Timestamp | Interaction Logging (User-created) | Always | Mandatory |
| Next Action Schedule | In-Progress Leads | If status = In-Progress | Mandatory |
| ClosureReason | Lead/Prospect Closure | Always | Mandatory |
| APC/ANC Code | Lead/Prospect Closure | Always | Mandatory per LP8.13 |

### 8.2 Format Validations

| Field | Format Rule | Error |
| --- | --- | --- |
| PAN | Standard alphanumeric format: 5 letters + 4 digits + 1 letter (AAAAA9999A) | Format error → reject |
| Aadhaar | Exactly 12 digits; must pass Verhoeff check-digit algorithm | COM103 "Invalid Aadhaar Number" |
| Passport | Standard format per issuing authority | Format error |
| Voter ID | Standard format | Format error |
| GSTIN | Standard 15-character format | Format error |
| Mobile Number (Primary) | Standard 10-digit Indian mobile format (starting 6–9); numeric only; valid country code if prefixed | COM112 (country code), COM113 (format), COM114 (non-numeric), COM115 (length) |
| Mobile Number (Alternate) | Same rules as primary mobile | COM112–COM115 |
| CKYC Number | Standard CKYC registry format | COM105 on uniqueness failure; format error on malformed input |
| Date Fields | DD/MM/YYYY | Parse error → reject |
| NRI Passport Validity | Not expired; valid for ≥ 90 days from submission date | "Passport is expired or expiring within 90 days" |
| Enterprise Customer ID | CUST-YYYY-NNNNNN | Generated by system only; not user-entered |
| Temp Customer Number | TMP-YYYY-NNNNNN | Generated by system only |
| LRN | LOB-YYYYMM-NNNNNN | Generated by system only |

### 8.3 Business Validations

| Rule | Condition | Error / Action |
| --- | --- | --- |
| Asset Cost > 0 | Asset cost = 0 or negative | Error LN3574: "Asset Cost Should Be Greater Than Zero" |
| Asset Cost ≥ Lease Amount | Asset cost < requested lease/finance amount | "Asset Cost Should be greater than or Equal to Finance Amount" |
| Asset Model Year Range | Asset category 1; model year outside range from system parameter HP024 | Rejection |
| GECL/Active Lease Block | Lessee has a blocking active lease (system parameter enabled) | Error LN4323 equivalent |
| Deceased Lessee | Lessee marked deceased in customer records | Error LN3574: "This Customer is deceased" |
| Digital Lease Channel Restriction | Users with digital channel access attempting restricted lease types outside that channel | Rule 54 equivalent: "Use the Digital Lease system for this lease type" |
| NRI Product Restriction | NRI lessee attempting non-permitted product combination | Rejection |
| Geographic Validation at Lead Creation | Pincode provided at Lead creation is outside branch service area (default 35 km); no bypass applies | Mobile: warning LN3955 (conditional save); Other channels: hard error LN3955 |
| Geographic Validation at Prospect Stage | Lessee registered pincode outside branch service area (default 35 km) at Prospect creation; no bypass applies | Mobile: warning LN3955 (conditional save); Other channels: hard error LN3955 |
| Aged Pending Lead Block | Branch has leads with null follow-up dates older than system parameter threshold | Error LN4924: specifies resolution deadline |
| Aged Pending Day-of-Month | Aged pending block is triggered on the configured day-of-month (system parameter AGED_PENDING_DOM); before that day in the current month, aged leads are reported but do not block creation | LN4924 only enforced from configured DOM onwards |
| Maximum Pending Days Block | Marketing employee or branch has pending items older than configured thresholds | Error LN3574 with category and age detail |
| Source-of-Business Restriction | Submitting user's source code not registered/active for branch | Rejection per LP2.10 |
| CKYC Uniqueness | CKYC number already linked to another customer | COM105 |
| Mobile Validation | Mobile fails country code / format / numeric / length checks | COM112 / COM113 / COM114 / COM115 |
| One-Branch Restriction | Promotion attempted from branch other than assigned branch | LN5098 |
| Branch Not Activated | Branch activation date is in the future | LN5099 equivalent |
| Three-Check Gate | Unresolved caution, fraud flag, or expired source-of-business at promotion | Specific reason code per failing check |
| CIBIL Re-initiation Excess | Re-initiation attempts exceed configured maximum | LN3780: Central Team approval required |
| CIBIL Same-Day Re-initiation | Second CIBIL request on same day after successful response | LN3782: user confirmation required |

---

## 9. Exception Handling

| Exception ID | Trigger | Category | Response | Outcome |
| --- | --- | --- | --- | --- |
| EX-01 | User or device authentication fails | Authentication failure | Submission rejected before any data processing | Request rejected with reason |
| EX-02 | Lead input payload absent or blank | Missing information | Submission rejected before any processing | Request rejected |
| EX-03 | Lease/contract type absent from payload | Missing information | Submission rejected | Request rejected |
| EX-04 | Minimum identifier rule not met | Missing information | Record routed to Exception Queue (not hard rejection) | Exception Queue entry created |
| EX-05 | Aadhaar fails Verhoeff algorithm or ≠ 12 digits (COM103) | Invalid input | Partial transaction rolled back; failure response returned | Request rejected |
| EX-06 | PAN on caution/block list — LN5337 raised; caution email alert dispatched | Business rule violation | Error LN5337 raised; partial transaction rolled back | Request rejected |
| EX-07 | Confirmed duplicate KYC (COM110) or active application match | Business rule violation | Partial transaction rolled back; failure response | Request rejected |
| EX-08 | Potential duplicate KYC (COM111) | Business rule violation | Informational warning; processing halted for review | Halted for review |
| EX-09 | DedupLabel = Conflict at Prospect promotion | Business rule violation | Prospect promotion blocked; route to exception queue | Blocked pending resolution |
| EX-10 | PAN/GSTIN validation service failure | Validation failure | Route Prospect to exception queue; Prospect stays Draft | Exception queue |
| EX-11 | Geographic validation: pincode outside service area (default 35 km) | Business rule violation | Mobile: warning (conditional save, indicator = 'Y'). Other channels: hard error LN3955 | Conditional or rejection |
| EX-12 | Asset cost ≤ 0 or < lease amount | Business rule violation | Partial transaction rolled back | Request rejected |
| EX-13 | NRI product/passport/contact validation failure | Business rule violation | Partial transaction rolled back | Request rejected |
| EX-14 | Active lease block (GECL equivalent) | Business rule violation | LN4323 equivalent raised | Request rejected |
| EX-15 | Deceased lessee | Business rule violation | "This Customer is deceased" | Request rejected |
| EX-16 | Aged pending leads block (enforced from configured day-of-month) | Business rule violation | LN4924 raised; specifies resolution deadline | Request rejected |
| EX-17 | LRN/Prospect ID generation lock timeout | Processing failure | Error D283: "Document Number In Use" | Request rejected; retry |
| EX-18 | Fraud detection service error (Hunter/Sherlock) | Processing failure | Fraud status recorded as error; application saved in Pending; autonomous log entry created | Processing continues with notation |
| EX-19 | Credit bureau service unavailable (non-2xx) | Processing failure | Logged; indicator set to "Credit Bureau Request not Submitted"; autonomous log entry created | Processing continues with notation |
| EX-20 | Modification blocked — lease contract in progress | Business rule violation | LN3713 equivalent | Request rejected |
| EX-21 | Modification blocked — fraud investigation active | Business rule violation | LN3785 equivalent | Request rejected |
| EX-22 | Invalid branch (not active) | Invalid input | LN480 raised | Request rejected |
| EX-23 | SMS dispatch failure | Processing failure | Logged silently; transaction continues | Processing continues silently |
| EX-24 | DAN invalid or already used | Invalid input | LN4468 / LN4470 raised | Request rejected |
| EX-25 | Risk category restricted for existing customer | Business rule violation | Error raised; process halted | Request rejected |
| EX-26 | Quote lock violation (edit without approval) | Business rule violation | Edit blocked; approval workflow required | Blocked |
| EX-27 | Customer creation gate not met (KYC or CAM incomplete) | Business rule violation | Customer ID creation blocked | Blocked |
| EX-28 | Lead / Application record persistence failure | Processing failure | Partial transaction rolled back; failure response with specific error message returned to caller | Request rejected with reason |
| EX-29 | Quote model price retrieval failure | Processing failure | Partial transaction rolled back; failure response returned; quote cannot be generated until service recovers | Request rejected with reason |
| EX-30 | Appraisal category validation failure | Business rule violation | Partial transaction rolled back; failure response returned; user must select a valid appraisal category | Request rejected with reason |
| EX-31 | Mobile number fails COM112 (invalid country code prefix) | Invalid input | Field-level validation error COM112; submission rejected | Request rejected |
| EX-32 | Mobile number fails COM113 (invalid format) | Invalid input | Field-level validation error COM113; submission rejected | Request rejected |
| EX-33 | Mobile number fails COM114 (non-numeric characters) | Invalid input | Field-level validation error COM114; submission rejected | Request rejected |
| EX-34 | Mobile number fails COM115 (insufficient length) | Invalid input | Field-level validation error COM115; submission rejected | Request rejected |
| EX-35 | CKYC number already linked to another customer (COM105) | Business rule violation | Error COM105 raised; submission rejected | Request rejected |
| EX-36 | Source-of-business code not registered or not authorised for branch | Business rule violation | Submission rejected per LP2.10 | Request rejected |
| EX-37 | Branch activation date in future — LN5099 | Invalid input | Branch not yet active; submission rejected | Request rejected |
| EX-38 | One-branch restriction violation — LN5098 | Business rule violation | Promotion from non-assigned branch blocked | Request rejected |
| EX-39 | Three-check gate failure at Prospect promotion | Business rule violation | Promotion blocked; specific failing check code returned to user | Blocked pending resolution |
| EX-40 | Fraud applicant status FA — immediate halt | Business rule violation | Applicant record flagged FA; submission halted; Central Team escalated | Halted; escalated |
| EX-41 | Fraud applicant status F — restricted processing | Business rule violation | Applicant record flagged F; routed to fraud review queue | Restricted pending review |
| EX-42 | Internal KYC dedup — LN4084 (duplicate PAN on lead) | Business rule violation | Alert raised; routing determined by associated record status | Alert; conditional |
| EX-43 | Internal KYC dedup — LN4088 (duplicate PAN on application) | Business rule violation | Alert raised; routing determined by associated record status | Alert; conditional |
| EX-44 | CIBIL re-initiation exceeds configured maximum — LN3780 | Business rule violation | Re-initiation blocked; Central Team approval required | Blocked pending approval |
| EX-45 | CIBIL same-day re-initiation — LN3782 | Business rule violation | Informational warning; explicit user confirmation required to proceed | Conditional proceed |

---

## 10. Data Entities & Attributes

### 10.1 Lead Record

| Attribute | Level | Description | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Temp Customer Number | Lead | Temporary identifier at base lead creation | Yes | Format: TMP-YYYY-NNNNNN |
| Lead Reference Number (LRN) | Lead | Unique tracking identifier | Yes | Format: LOB-YYYYMM-NNNNNN |
| Lead Type | Lead | Individual or Commercial | Yes | If Commercial, Contact Person required |
| Company (Known As) | Lead | Commercial entity name | Conditional | If Lead Type = Commercial |
| Contact Person | Lead | Primary contact for commercial leads | Conditional | If Lead Type = Commercial |
| Source Category | Lead | Internal / External / Campaign / Dealer | Yes | Used for routing and reporting |
| Source Name | Lead | Specific source / campaign identifier | Yes | e.g., Diwali-CAR-LEASE-2025 |
| Source-of-Business Code | Lead | Registered source code for the submitting user | Yes | Validated against source-of-business master (LP2.10) |
| Mobile | Lead | Contact and dedup identifier | Conditional | One identifier required; COM112–COM115 validated |
| Alternate Mobile | Lead | Secondary contact | Conditional | Mandatory for NRI; COM112–COM115 validated |
| Email | Lead | Contact and dedup identifier | Conditional | One identifier required |
| PAN | Lead | Tax identifier (individual) | Optional at Lead; Mandatory at Prospect | Dedup and existence check |
| GSTIN | Lead | Tax identifier (commercial) | Optional at Lead; Mandatory at Prospect | Dedup and existence check |
| CKYC Number | Lead | CKYC registry identifier | Optional | Uniqueness validated; COM105 on duplicate (NEW v2.0) |
| Aadhaar | Lead | Biometric identifier (individual) | Optional | Verhoeff validated (COM103); 12 digits |
| Driving Licence | Lead | KYC identity document | Optional | Format validated |
| Passport Number | Lead | KYC identity document | Conditional | Mandatory for NRI |
| Passport Validity Date | Lead | Passport expiry | Conditional | Mandatory for NRI; must be ≥ 90 days future |
| Voter ID | Lead | KYC identity document | Optional | Format validated |
| Address | Lead | Address / location details | Conditional | One identifier required |
| Address Line 1 | Lead | Street / building | Optional | 3–40 characters |
| Address Line 2 | Lead | Area / locality | Optional | 3–40 characters |
| Pincode | Lead | Postal code for geographic validation | Optional | Used for branch-pincode mapping; 35 km default radius |
| Location Name | Lead | Location / city name | Conditional | Mandatory on Applicant records |
| Location/City | Lead | City for routing | Optional | Used for branch assignment |
| UCIC | Lead | Group-level customer identity | Conditional | Displayed on dedup match with full profile |
| Existing Customer Codes | Lead | Mapped customer codes under UCIC | Conditional | Multi-code handling |
| DedupLabel | Lead | PossibleExisting / Unknown / Conflict / New | Yes | Mandatory before promotion |
| Gender | Lead | M / F (Individual); Cleared (Non-Individual) | Conditional | Normalised at entry |
| Date of Birth | Lead | Applicant DOB | Optional | DD/MM/YYYY |
| Constitution Type | Lead | Individual / Non-Individual | Yes | Governs validation rules |
| Residential Type | Lead | Resident / NRI | Optional | Triggers NRI rules |
| PAN Exemption Flag | Lead | Y / N | Yes | Derived from DAN |
| DAN | Lead | Declaration Account Number (PAN exemption) | Conditional | Must be valid and unused |
| Occupation | Lead | Applicant occupation | Optional | Male + HOUSE WIFE combination invalid |
| Enquiry Decision Indicator | Lead | APC / ANC | Conditional | Set on closure per LP8.13 (NEW v2.0) |
| Fraud Status Code | Lead/Applicant | FA / F / Clear | Conditional | Set by fraud screening; FA blocks, F restricts (NEW v2.0) |
| Assigned Branch / Team | Lead | Current owner | Yes | Default CPU |
| AssignedHierarchyLevel | Lead | Central / Branch / Officer | Yes | For SLA monitoring |
| Record Status | Lead | New / Assigned / In-Progress / Promoted / Closed | Yes | Drives allowable actions |
| Lead Temperature | Lead | Cold / Warm / Hot | Yes | System suggested + override |
| StatusSuggestedBy | Lead | System / Manual | Yes | Audit required |
| StatusReason | Lead | Reason for manual override | Conditional | Mandatory on override |
| ClosureReason | Lead | Reason for closing | Conditional | Mandatory on closure |
| AttemptCounters | Lead | Calls / Messages / Emails count | Optional | Feeds temperature engine |
| NextMeetingDetails | Interaction | Next follow-up schedule | Conditional | Mandatory for In-Progress |
| Geotag | Lead | Latitude / Longitude | Optional | Mobile creation |
| SMS Indicator | Lead | Y / N | Yes | Derived from decision indicator |

### 10.2 Prospect Record

| Attribute | Level | Description | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Prospect ID | Prospect | System-generated unique identifier | Yes | Format: PR-YYYY-NNNNNN |
| Base Link | Prospect | Link to Lead LRN and Temp Customer No | Yes | Lineage |
| PAN | Prospect | Tax identifier (individual) | Conditional | Mandatory for Individual |
| GSTIN | Prospect | Tax identifier (commercial) | Conditional | Mandatory for Commercial |
| CKYC Number | Prospect | CKYC registry identifier | Optional | Carried from Lead; uniqueness maintained (NEW v2.0) |
| Legal Entity Name | Prospect | Validated legal name | Yes | From PAN/GST validation service |
| Alias / Brand Name | Prospect | Known-as name | Optional | Stored alongside legal name |
| Registered Address | Prospect | Validated registered address fields | Yes | Line1 / City / State / Pincode |
| Owner | Prospect | Current owner (branch/team/user) | Yes | From assignment |
| Prospect Status | Prospect | Draft / Validated / Active / InAppraisal / CustomerCreated / Closed | Yes | See Section 11 |
| Validation Source | Prospect | Source of PAN/GSTIN validation | Yes | Metadata: source, timestamp, reference |
| Processing Mode | Prospect | Digital / Manual / Bulk | Yes | Channel mode carried from lead creation (NEW v2.0) |
| Meeting ID | Meeting | System-generated identifier | Yes | Format: MTG-YYYY-NNNNN |
| Meeting Date & Time | Meeting | Scheduled timestamp | Yes | Mandatory |
| Attendees | Meeting | Names and designations | Optional | |
| Notes | Meeting | Meeting notes and outcome | Yes | Mandatory |
| Opportunity ID | Opportunity | Unique identifier | Yes | Format: OPP-YYYY-NNNNNN |
| LoB Tag | Opportunity | Leasing / Lending / Deposits / Others | Yes | Mandatory |
| Asset Category | Opportunity | High-level asset category | Yes | Mandatory |
| Asset Class | Opportunity | Asset class | Yes | Mandatory |
| Contract Type | Opportunity | Lease/contract type code | Yes | Validated against master; Vruddhi types flagged (NEW v2.0) |
| Asset Make | Opportunity | Manufacturer | Optional | |
| Asset Model | Opportunity | Model/Variant | Optional | |
| Quote ID / Version | Quote | Quote identifier and version | Conditional | Multiple per Opportunity allowed |
| Quote Status | Quote | Draft / Shared / Locked | Yes | Locked controls edits |
| Asset Cost | Quote | Market value (NDLP or entered) | Yes | System may override with market value |
| Lease / Finance Amount | Quote | Requested finance quantum | Conditional | Must be ≤ asset cost |
| Application ID | Application | Origination identifier | Conditional | When initiated |
| KYC Status | Application | Not Started / In Progress / Complete | Conditional | Gate for customer creation |
| CAM Status | Application | Not Started / In Progress / Approved / Declined | Conditional | Gate for customer creation |
| Sanction ID | Sanction | Sanction package identifier | Conditional | Issued by Central Team |
| Hunter Result | Application | Fraud screening result | Conditional | NH/HT/FA/EX/NA |
| Sherlock Result | Application | Fraud screening result | Conditional | AP/RE/RJ/FA/TM/IP/EX |
| Fraud Status Code | Application/Applicant | FA / F / Clear | Conditional | See LP4.12 (NEW v2.0) |
| CIBIL Status | Application | Credit bureau request status | Conditional | |
| CIBIL Re-initiation Count | Application | Number of CIBIL re-initiation attempts | Conditional | Tracked per LN3780 (NEW v2.0) |
| Enterprise Customer ID | Customer | Generated on KYC+CAM gate | Conditional | Format: CUST-YYYY-NNNNNN |

### 10.3 Interaction / Audit Log

| Attribute | Level | Description | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Interaction ID | Log | System-generated unique ID | Yes | |
| Entity Type | Log | Lead / Prospect / Application | Yes | |
| Entity ID | Log | LRN / Prospect ID / Application ID | Yes | |
| Interaction Type | Log | Call / Email / Meeting / SMS / SYSTEM | Yes | 'SYSTEM' for autonomous entries |
| Timestamp | Log | Date and time of interaction | Yes | |
| Operator | Log | User ID or 'SYSTEM' for autonomous entries | Yes | |
| Outcome / Notes | Log | Free text description | Yes | |
| Contact Person | Log | Name and designation | Conditional | Commercial leads |
| Next Action Date | Log | Next follow-up date | Conditional | In-Progress leads |
| Next Action Mode | Log | Contact mode for next interaction | Conditional | In-Progress leads |
| Autonomous Flag | Log | Y / N | Yes | Y for system-generated entries per LP6.6 (NEW v2.0) |

### 10.4 Branch Entity (NEW v2.0)

| Attribute | Level | Description | Mandatory | Notes |
| --- | --- | --- | --- | --- |
| Branch Code | Branch | Unique identifier for the branch | Yes | Used in LRN construction |
| Branch Name | Branch | Legal name of the branch | Yes | |
| Branch Type | Branch | Regular / SME | Yes | SME branches exempt from geographic validation |
| Segment Code | Branch | 'SM' for SME; other codes for regular | Yes | |
| Activation Date | Branch | Date from which the branch may process records | Yes | Must be ≤ processing date; LN5099 if future |
| Business Open Date | Branch | Date branch business was opened | Yes | Must be set for branch to be active |
| Accounting Open Date | Branch | Date accounting was opened | Yes | Must be set for branch to be active |
| Close Date | Branch | Date branch was closed (if applicable) | Conditional | Null = active |
| Max Service Radius (km) | Branch | Maximum geographic service radius | Yes | Default 35 km; branch-specific override |
| Approval Stage Count | Branch | 1 = single-stage; 2 = two-stage | Yes | Governs PP7.9 and PP7.13 |
| Two-Stage Email Recipient | Branch | Email address for two-stage branch notification | Conditional | Mandatory if Approval Stage Count = 2 |
| Pincode Override List | Branch | Explicit pincodes mapped to this branch outside radius | Optional | Bypasses distance check for listed pincodes |

---

## 11. Status Models & Transitions

### 11.1 Lead Status Model

| Entity | From Status | To Status | Trigger | Key Validations |
| --- | --- | --- | --- | --- |
| Lead | New | Assigned | Assignment completed (default CPU or self-assign) | No unassigned leads permitted |
| Lead | Assigned | In-Progress | First interaction logged | Timestamp + interaction type mandatory |
| Lead | In-Progress | Promoted | Qualification complete; PAN/GSTIN mandatory; DedupLabel resolved; three-check gate passed | Full promotion checklist (see LP8) |
| Lead | In-Progress | Closed | Closure reason recorded; APC/ANC code set | ClosureReason mandatory; APC/ANC mandatory; record locked |
| Lead | Assigned | Closed | Closure reason recorded; APC/ANC code set | ClosureReason mandatory |

### 11.2 Lead Temperature Model

| Temperature | Trigger | Override |
| --- | --- | --- |
| Hot | High engagement; confirmed intent; critical fields completed | Manual override allowed; StatusReason mandatory |
| Warm | Moderate engagement; interest expressed but unconfirmed | Manual override allowed; StatusReason mandatory |
| Cold | Low engagement; multiple unanswered attempts; inactivity threshold reached | Manual override allowed; StatusReason mandatory |

Degradation rules:
- Hot → Warm: Configurable inactivity period (default: 7 days). Autonomous log entry generated per LP6.6.
- Warm → Cold: Configurable inactivity period (default: 14 days). Autonomous log entry generated per LP6.6.

### 11.3 Prospect Status Model

| Entity | From Status | To Status | Trigger | Key Validations |
| --- | --- | --- | --- | --- |
| Prospect | Draft | Validated | PAN/GSTIN validation success (PP3) | Validation service returns success |
| Prospect | Validated | Active | Assignment complete and first meeting/interaction logged (PP2/PP4) | Remarks mandatory on assignment |
| Prospect | Active | InAppraisal | Application initiated and KYC/CAM started (PP7) | Application ID created |
| Prospect | InAppraisal | CustomerCreated | KYC = Complete and CAM = Approved (PP8) | Both gates simultaneously satisfied; APC code set on applicants |
| Prospect | Any | Closed | Closure reason recorded; APC/ANC code set | ClosureReason mandatory |

### 11.4 Opportunity & Quote Status Model

| Entity | From Status | To Status | Trigger |
| --- | --- | --- | --- |
| Opportunity | Open | Quoted | First quote attached |
| Opportunity | Quoted | Negotiation | Customer requests revision |
| Opportunity | Negotiation | QuoteLocked | Final quote confirmed and locked |
| Opportunity | QuoteLocked | Converted | Application initiated |
| Opportunity | Any | Closed | Opportunity closed with reason |
| Quote | Draft | Shared | Quote sent to lessee |
| Quote | Shared | Locked | Final quote confirmed |
| Quote | Locked | Draft | Re-negotiation approved (Manager/Central Team) |

### 11.5 Application Status Model

| Entity | From Status | To Status | Trigger |
| --- | --- | --- | --- |
| Application | Initiated | InReview | Submission complete; documents uploaded |
| Application | InReview | Approved | CAM approval granted |
| Application | InReview | Rejected | CAM declined |
| Application | InReview | Pending | Fraud screening non-clear; awaiting manual review |
| Application | Pending | InReview | Fraud review cleared manually |

### 11.6 Applicant Status Model (NEW v2.0)

| Entity | Status Code | Meaning | Trigger |
| --- | --- | --- | --- |
| Applicant | (blank / active) | Active in current lead/application | Default state |
| Applicant | ANC | Applicant Negative Closed | Lead/Prospect closed with negative outcome (LP8.13) |
| Applicant | APC | Applicant Positive Closed | Customer created (PP8.4) or lead closed with positive outcome (LP8.13) |
| Applicant | FA | Fraudulent Applicant | Hunter/Sherlock returns FA result (LP4.12) |
| Applicant | F | Fraud Flag Active | Fraud flag raised but not confirmed; restricted processing |

---

## 12. Integration Touchpoints

| Integration | Purpose | Key Inputs | Key Outputs | Status |
| --- | --- | --- | --- | --- |
| Authentication Service | Validate user identity and device | User ID, Device IMEI, Device UUID, App Version | User status, error code/message | Required |
| UCIC Mapping Service | Resolve UCIC and existing customer codes from PAN/GSTIN/Mobile/Email/CKYC | Identity identifiers | UCIC, customer code list, dedup match, full customer profile | Required |
| External PAN De-duplication API | Check PAN against external dedup database for non-OEM leads (MSIL/TMF/TAFE/HYUNDAI exempt) | PAN, constitution type, business source | COM110 / COM111 / COM66 / clear | Required |
| PAN Validation Service | Validate PAN and fetch legal name/address | PAN number | Legal name, address, validation metadata | Required |
| GSTIN Validation Service | Validate GSTIN and fetch legal entity name/address | GSTIN | Legal entity name, registered address, derived PAN | Required |
| Caution List Service | Check PAN against internal caution/block database; trigger email alert on match | PAN | Status: Allowed / Blocked / Caution; reason; email alert | Required |
| Caution Alert Email Service | Dispatch email alerts to monitoring team on caution-list match | Applicant details, match reason | Email delivery status | Required |
| Pincode-Branch Mapping | Validate applicant pincode against branch service area (default 35 km) | Company code, branch code, pincode, location ID | Validation result, distance, error LN3955 | Required |
| Source-of-Business Master | Validate source code is registered and active for target branch | Source code, branch code | Active Y/N, authorised Y/N | Required |
| Product Model Price Service | Retrieve current NDLP/market value for product model | Lease type, asset class, dealer code, location, model, qty | Market value, advisory message | Required |
| Appraisal Category Validation | Validate assessment category against branch/product parameters | Branch, category, lease type, asset class, market value, date | Validation result | Required |
| Enquiry/Application Persistence | Create or modify lead/prospect/application records; generate IDs | All validated fields | Lead ID / Prospect ID / Application ID, error | Required |
| Lead Management System Update | Update LMS record with generated Prospect ID | Company code, User ID, LMS lead ID, Prospect ID | Success/error | Required |
| Hunter / Sherlock (Fraud) | Fraud screening post-application creation | Company code, application number | Hunter result, Sherlock result, FA/F indicators | Required |
| Credit Bureau (CIBIL) | Credit report request for individual lessees; re-initiation governed by LN3786/LN3780/LN3782 | Parameterised request | 2xx = success; status indicator; re-initiation count | Required |
| Document Management System (DMS) | Upload KYC document images | Encoded image, XML metadata, environment indicator | Document index | Required |
| Two-Stage Branch Email Service | Dispatch application summary email to Branch Head for two-stage branches | Application ID, applicant name, branch contact | Email delivery status | Required |
| Reminder / Calendar Service | Meeting reminders for Prospects | Meeting date/time, attendees | Notification/calendar entry | Optional |
| Contract Type Master | Validate contract type codes including Vruddhi types (12/22/41/42/43/44) | Contract type code | Valid Y/N, Vruddhi flag | Required |
| Branch Master | Validate branch activation date, approval stage, service radius | Branch code | Activation date, stage count, radius, close date | Required |

---

## 13. Dashboards, SLAs & KPIs

### 13.1 Role-Based Dashboards

| Dashboard | Audience | Key Metrics |
| --- | --- | --- |
| My To-Do List | All associates / Field Officers | Leads assigned today; follow-ups due; pending interactions |
| Lead Pipeline | Branch Manager / CPU | Total leads by status; temperature distribution; conversion to Prospect |
| Prospect Pipeline | Branch Manager / CPU / Central Team | Prospects by status; opportunities open/quoted/locked; applications in appraisal |
| Exception Queue | CPU / Operations | Open exceptions; aged exceptions; exception resolution TAT |
| SLA Compliance | All managers | SLA breaches by level; escalation count; average first-contact TAT |
| Aging Report | Branch Manager / RVP | Leads by days-since-creation buckets; Prospects by days-in-status |
| Performance Dashboard | RVP / Regional / Branch Heads | Conversion rates (Lead→Prospect→Customer); closures by reason; data completeness scores |
| Marketing Source Report | Operations / Business | Leads and conversions by Source Category/Source Name |
| Fraud & Caution Report (NEW v2.0) | CPU / Operations / Central Team | Caution-list matches by period; fraud flag counts (FA/F); caution email alerts dispatched |
| APC/ANC Closure Report (NEW v2.0) | Operations / Business | Lead/Prospect closures by APC vs ANC; ANC reason distribution |

### 13.2 KPIs

| KPI | Definition |
| --- | --- |
| Lead Aging | Days since lead creation; bucketed: 0–7 / 8–15 / 16–30 / 30+ days |
| First-Contact TAT | Time from lead assignment to first interaction logged |
| Attempts per Lead | Total call/message/email attempts per lead before conversion or closure |
| Response Rate | % of leads with at least one positive lessee response |
| Data Completeness Score | % of optional fields completed across all leads in period |
| Lead-to-Prospect Conversion Rate | % of leads promoted to Prospect |
| Prospect-to-Customer Conversion Rate | % of Prospects where customer creation is completed |
| Closure Reason Distribution | Count and % of leads/prospects closed by each reason code |
| Exception Cure Rate | % of exception queue items resolved within SLA |
| Quote Lock Rate | % of Opportunities reaching QuoteLocked status |
| CAM Approval Rate | % of applications approved by CAM |
| APC/ANC Ratio (NEW v2.0) | Ratio of positive closures (APC) to negative closures (ANC) per period |
| Caution Match Rate (NEW v2.0) | % of leads/applications triggering a caution-list match |

### 13.3 SLA Rules

| SLA | Trigger | Threshold | Escalation Target |
| --- | --- | --- | --- |
| CPU First Assignment | Lead creation / ingestion | 4 hours | Operations Officer |
| Branch-to-Officer Assignment | Lead assigned to Branch Manager | 24 hours | Branch Manager escalated to RVP |
| First Interaction after Assignment | Lead assigned to Field Officer | 48 hours | Branch Manager |
| Exception Cure | Record placed in Exception Queue | 72 hours (configurable) | CPU / Central Team |
| Prospect PAN/GSTIN Validation Exception | Prospect in Draft status | 24 hours | Central Team |
| FO Absence Escalation | FO marked absent with open leads | Immediate | Branch Manager |
| Aged Pending Lead Block | Lead follow-up date null; created before system-parameter threshold; enforced from configured day-of-month | Configurable per month-day rule (AGED_PENDING_DOM) | Creation blocked at branch until resolved |
| Two-Stage Branch Email (NEW v2.0) | Application created at two-stage branch | Immediate dispatch | N/A (monitoring only) |
| Caution Alert Email (NEW v2.0) | Caution-list match detected at any stage | Immediate dispatch | N/A (monitoring only) |

---

## 14. Glossary

| Term | Definition |
| --- | --- |
| Lead | A formally recorded expression of interest in a leasing product, capturing minimum contact identifiers and assigned a unique Lead Reference Number (LRN). |
| Lead Reference Number (LRN) | Unique system-generated identifier for a leasing lead. Format: LOB-YYYYMM-NNNNNN. Generated by lock-protected atomic increment. |
| Temp Customer Number | A temporary identifier assigned at base lead creation before full identity validation. Format: TMP-YYYY-NNNNNN. |
| Prospect | A lead that has been promoted after PAN/GSTIN validation; assigned a Prospect ID; represents a lessee actively being assessed for a leasing arrangement. |
| Prospect ID | Unique system-generated identifier for a leasing prospect. Format: PR-YYYY-NNNNNN. Generated only after successful PAN/GSTIN validation. |
| Lessee | The primary party in a leasing arrangement (individual or corporate) who will use the leased asset. Equivalent to 'Borrower'/'Main Applicant' in the lending domain. |
| Additional Applicant | A co-lessee or guarantor added to a lead/prospect record alongside the main lessee, numbered sequentially: ADDL APPLICANT - 1, 2, 3, etc. |
| DedupLabel | The result of the de-duplication check against existing records: PossibleExisting / New / Unknown / Conflict. |
| UCIC | Unique Customer Identification Code — group-level identity linking multiple customer codes for the same entity across lines of business. |
| PAN | Permanent Account Number — national tax identifier for individuals and entities; primary KYC document for individual lessees. |
| GSTIN | GST Identification Number — 15-character tax identifier for businesses; primary KYC document for commercial lessees. |
| CKYC | Central Know Your Customer — a centralised KYC registry maintained by the government/regulator. A CKYC number uniquely identifies a KYC-verified individual or entity across financial institutions. CKYC numbers are optional at Lead stage and included in the de-duplication set. |
| Aadhaar | 12-digit national biometric identifier; validated using the Verhoeff check-digit algorithm (failure: COM103). |
| Verhoeff Algorithm | A check-digit algorithm applied to Aadhaar numbers using permutation and multiplication tables; a result of zero indicates a valid number. Failure raises COM103. |
| DAN | Declaration Account Number — used by applicants exempt from providing PAN; each DAN may only be used once. |
| NRI | Non-Resident Indian — subject to additional product restrictions, mandatory passport validity requirements, and contact information obligations. |
| Active Lease Block | A policy-driven condition (system parameter-enabled) that prevents creation of a new leasing enquiry for a lessee holding a qualifying existing active lease. Equivalent to GECL block in lending. |
| Caution List | An internal database of PAN numbers and customer identifiers flagged for restricted or blocked status; checked before accepting any lead or prospect. Matches trigger automatic email alerts to the monitoring team. |
| NDLP | National Dealer List Price — the standardised dealer/market price for a product model in a given location, fetched from the system and used to override the submitted asset cost where the NDLP differs. |
| Opportunity | A LoB-specific record attached to a Prospect capturing asset interest, quotes, and downstream application references. One Prospect may have multiple Opportunities (one per LoB). |
| Rack Rate Quote | A standard lease quote retrieved from the Quote Master based on asset taxonomy and standard pricing slabs; does not require additional approval. |
| Customised Quote | A quote that deviates from the rack rate; requires Manager or Central Team approval before it can be shared with the lessee. |
| Quote Lock | The state of a quote that has been confirmed as final; post-lock edits require explicit approval and are auditable. |
| CAM | Credit Appraisal Memo / Credit Appraisal — the credit assessment process for a leasing prospect, generating sanctioned limits and conditions. |
| KYC | Know Your Customer — the set of identity verification documents (PAN, Aadhaar, Passport, Driving Licence, Voter ID, GSTIN, CKYC) required to establish a lessee's identity. |
| DMS | Document Management System — the external system to which KYC images are uploaded under the application reference, accessed via environment-specific endpoints (Test/Beta/Live). |
| Hunter / Sherlock | External fraud detection services that screen application and applicant data against fraud databases; both results are evaluated together to determine fraud clearance. Result codes include FA (fraudulent) and F (fraud flag). |
| CIBIL | The external credit reporting service to which a bureau request is submitted following successful application creation for individual lessees. Re-initiation is governed by LN3786/LN3780/LN3782. |
| CPU | Central Processing Unit (Team) — the first-line team responsible for lead ingestion, default ownership, exception queue management, and SLA monitoring. |
| LoB | Line of Business — a distinct business unit: Leasing, Lending, Deposits, Others. |
| SME Branch | A branch designated with segment code 'SM' for which pincode geographic validation is bypassed. |
| Branch Stage | Configuration parameter indicating single-stage (1) or two-stage (2) credit approval at a branch, affecting when an application advances to Eligible for Application status and whether a two-stage email is dispatched. |
| Eligible for Application (EA) | Application status indicating all validations have passed and the application is ready for formal processing. |
| Pending (PE) | Application status indicating the application has been created but is awaiting fraud or credit check completion. |
| OEM / Partner Source | An Original Equipment Manufacturer or registered partner whose leads are exempt from certain external PAN de-duplication checks. Named exemptions: MSIL, TMF, TAFE, HYUNDAI. |
| SLA | Service Level Agreement — time-bound action and escalation rules governing each stage of the lead and prospect lifecycle. |
| Lineage | The immutable chain of cross-reference links: Lead → Prospect → Opportunity → Quote → Application → Enterprise Customer. |
| APC | Applicant Positive Closed — applicant status code set when a lead or prospect reaches a positive outcome (customer creation or positive closure). Recorded on each applicant record at closure. |
| ANC | Applicant Negative Closed — applicant status code set when a lead or prospect is closed with a negative outcome. ANC records are preserved in the consent chain per PP7.10 and trigger LN4926. |
| Enquiry Decision Indicator | Field on applicant records capturing the final outcome: APC (positive) or ANC (negative). Set at lead/prospect closure. |
| Autonomous Transaction | A system-generated log entry created without user action, recording a system-triggered event (temperature degradation, SLA breach, caution match, fraud screening result). Carries operator='SYSTEM' and is immutable. |
| Vruddhi | Collective term for special lease/contract type codes (12, 22, 41, 42, 43, 44) that require additional validation at quote and application stages and must be communicated to the downstream contract generation system. |
| Processing Mode | Attribute capturing the origination channel of a lead/prospect: Digital / Manual / Bulk. Governs digital channel lease type restrictions. |
| Source-of-Business | A registered code identifying the sales/marketing source through which a lead is originated. Must be active and authorised for the branch before a lead can be created. |

---

## 15. Configuration Parameters Reference (NEW v2.0)

This section documents system configuration parameters referenced in the business rules. Parameters are managed in the system parameter registry and are not hardcoded in the application.

| Parameter Code | Description | Default Value | Used In |
| --- | --- | --- | --- |
| HOT_WARM_INACTIVITY_DAYS | Days of inactivity before Hot degrades to Warm | 7 | LP7.3 |
| WARM_COLD_INACTIVITY_DAYS | Days of inactivity before Warm degrades to Cold | 14 | LP7.3 |
| LRN_LOCK_TIMEOUT_SECONDS | Maximum seconds to wait for LRN generation lock | 15 | LP2.3, LP3.3 |
| HP024 | Asset model year range parameter for asset category 1 | Configured per policy | LP8.7 |
| ACTIVE_LEASE_BLOCK_ENABLED | Enable/disable active lease blocking at lead/prospect creation | false | LP8.4, LP8 |
| NRI_PASSPORT_MIN_DAYS | Minimum passport validity days for NRI lessees | 90 | LP8.5 |
| GEOGRAPHIC_DEFAULT_RADIUS_KM | Default maximum service radius for branch geographic validation | 35 | LP2.8, PP3.5 |
| AGED_PENDING_DOM | Day of month from which aged-pending block is enforced | Configured per policy | EX-16, Section 8.3 |
| AGED_PENDING_THRESHOLD_DAYS | Days before a pending lead is considered aged | Configured per policy | LP7.4, EX-16 |
| SANCTION_VALIDITY_DAYS | Days a sanction remains valid for existing customers | 180 | PP7.8 |
| ELIGIBLE_LEASE_TYPES | Lease types triggering immediate EA status and welcome communication | FINANCE_LEASE, OPERATING_LEASE | PP7.9, PP7.12 |
| DIGITAL_PERMITTED_LEASE_TYPES | Lease/contract types permitted for digital channel origination | Configured per policy | LP2.13 |
| CIBIL_REINITIATION_WINDOW_DAYS | Days after a successful CIBIL response before re-initiation is permitted | Configured per policy | PP7.14 (LN3786) |
| CIBIL_MAX_REINITIATION_ATTEMPTS | Maximum CIBIL re-initiation attempts per application without Central Team approval | Configured per policy | PP7.14 (LN3780) |
| OEM_EXEMPTION_LIST | Named OEM/partner codes exempt from external PAN dedup | MSIL, TMF, TAFE, HYUNDAI | LP4.10 |
| VRUDDHI_CONTRACT_TYPES | Contract type codes classified as Vruddhi | 12, 22, 41, 42, 43, 44 | PP5.5, PP6.8, PP8.6 |
| CAUTION_ALERT_EMAIL_RECIPIENTS | Email addresses to receive caution-list match alerts | Configured per deployment | LP4.2, PP7.11, PP8.5 |
| TWO_STAGE_BRANCH_EMAIL_ENABLED | Enable/disable two-stage branch email notification | true | PP7.13 |
| EXCEPTION_CURE_SLA_HOURS | Configurable cure SLA for Exception Queue entries | 72 | LP3.4, Section 13.3 |
| BULK_BATCH_SIZE | Maximum records per bulk upload batch | 100 | LP3 |
| ENV_INDICATOR | DMS routing environment: T=Test, B=Beta, L=Live | T | PP7.3 |

---

## 16. Omissions & Out-of-Scope Notes (NEW v2.0)

This section documents rules from the source lending specification that are intentionally omitted from the leasing specification, with the rationale for each omission.

| Omitted Rule / Feature | Source Reference | Reason for Omission |
| --- | --- | --- |
| Hire Purchase (HP) specific product rules | Lending spec HP sections | HP is a lending product. Leasing covers Operating Lease, Finance Lease, and the Vruddhi subtypes (12/22/41/42/43/44) only. |
| CERSAI registration and charges creation | Lending spec CERSAI section | Security interest registration does not apply to operating and finance leases; no asset mortgage is created. |
| Loan account number and drawdown scheduling | Lending spec drawdown rules | Leasing uses periodic rentals, not drawdowns. Billing/rental scheduling is in the Lease Contract RS. |
| Co-borrower credit appraisal rules | Lending co-borrower sections | Leasing uses co-lessee construct; credit appraisal rules are applied at the CAM stage, not at lead/prospect. |
| Priority sector lending (PSL) classification | Lending PSL section | PSL classification applies to lending products only; not applicable to leasing. |
| NACH/ECS mandate setup | Lending mandate sections | Mandate setup is handled at lease contract stage, not at lead/prospect. |
| Insurance linkage rules | Lending insurance section | Asset insurance is captured at lease contract stage, not at lead/prospect origination. |
| Moratorium and holiday period rules | Lending schedule rules | Not applicable to leasing payment structures. |
| GSTIN de-registration / suspension detection | Lending GSTIN rules beyond format | Out of scope: GSTIN validation covers format and name/address enrichment only. |
| FATCA/CRS tax residency declaration | Lending FATCA section | FATCA/CRS declarations are handled at customer onboarding in the Customer Master RS, not at lead/prospect. |
| Multiple guarantor KYC chains | Lending guarantor rules | Guarantor structures for leasing are simpler; multiple guarantor KYC chain rules are addressed in the KYC RS. |
| Rate reset and repricing events | Lending rate reset section | Not applicable; leasing rates are fixed at quote lock and governed by the Lease Contract RS. |
