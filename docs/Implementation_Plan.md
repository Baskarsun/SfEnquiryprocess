# Leasing Lead Generation & Prospect Creation — Phased Implementation Plan

| Document Control | |
| --- | --- |
| Version | 1.0 |
| Date | 2026-04-28 |
| Status | Draft |
| Business Domain | Leasing — Lead-to-Prospect |
| Reference | Leasing_Lead_Prospect_Functional_Specification.md, DDD.xlsx, Enterprise Architecture.docx |

---

## Table of Contents

1. Implementation Overview
2. Architecture Blueprint
3. Infrastructure Components
4. Phase 1 — Foundation & Core Lead Management
5. Phase 2 — Lead Intelligence & Bulk Ingestion
6. Phase 3 — Prospect Creation & External Validation
7. Phase 4 — Opportunity, Quote & Application Origination
8. Phase 5 — Customer Creation, Lineage & Downstream Integration
9. Cross-Phase Concerns
10. Dependency Map
11. Risk Register

---

## 1. Implementation Overview

### Scope Summary

This plan covers end-to-end delivery of the **Leasing Lead-to-Customer** journey across two major functional domains:

| Domain | Processes |
| --- | --- |
| Lead Generation | LP1–LP8: Authentication → Lead Creation → Bulk Ingestion → Deduplication → Assignment → Interaction Logging → Qualification → Prospect Promotion |
| Prospect Creation | PP1–PP8: Prospect View → Assignment → PAN/GSTIN Validation → Meetings → Opportunities → Quotes → Application Origination → Customer Creation |

### Phasing Strategy

| Phase | Theme | Duration (Estimate) | Deliverable |
| --- | --- | --- | --- |
| Phase 1 | Foundation & Core Lead Management | 10 weeks | Authenticated lead capture, manual assignment, interaction logging, exception queue |
| Phase 2 | Lead Intelligence & Bulk Ingestion | 8 weeks | Bulk upload/API, deduplication, temperature engine, SLA/escalation, geographic validation |
| Phase 3 | Prospect Creation & External Validation | 8 weeks | Lead promotion, PAN/GSTIN validation, Prospect lifecycle, meeting management |
| Phase 4 | Opportunity, Quote & Application | 10 weeks | Opportunities, quote engine, application origination, KYC/CAM, DMS, fraud/credit bureau |
| Phase 5 | Customer Creation & Downstream | 6 weeks | Customer gate, lineage, role assignment, downstream triggers, full analytics |
| **Total** | | **~42 weeks** | |

---

## 2. Architecture Blueprint

### System Layers

```
┌──────────────────────────────────────────────────────────────────────┐
│  Presentation Layer                                                   │
│  Mobile App (iOS/Android)  ·  Web App (React/Angular)                │
│  Desktop PWA               ·  Admin Portal                           │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ HTTPS / REST / WebSocket
┌──────────────────────▼───────────────────────────────────────────────┐
│  API Gateway & BFF Layer                                              │
│  Rate Limiting  ·  Auth Token Validation  ·  Request Routing         │
│  Channel Tagging (Mobile/Desktop/API/Bulk)                           │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────────────┐
│  Microservices Layer (Domain-Aligned)                                 │
│                                                                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │  Lead Svc    │  │ Prospect Svc │  │  Assignment  │               │
│  │  (LP1–LP8)   │  │  (PP1–PP8)   │  │  & SLA Svc   │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │  Identity &  │  │ Opportunity  │  │    Quote     │               │
│  │  Dedup Svc   │  │    Svc       │  │    Svc       │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ Application  │  │  Notification│  │   Audit &    │               │
│  │   Svc        │  │    Svc       │  │  Lineage Svc │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
└──────────────────────┬───────────────────────────────────────────────┘
                       │ Internal Events (Kafka)
┌──────────────────────▼───────────────────────────────────────────────┐
│  Integration / Adapter Layer                                          │
│  PAN Validation · GSTIN Validation · UCIC Mapping                    │
│  External PAN Dedup API · Caution List · Pincode-Branch              │
│  Hunter/Sherlock · CIBIL · DMS · SMS/Email Gateway                   │
│  Product Price Service · Authentication Provider                      │
└──────────────────────┬───────────────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────────────┐
│  Data Layer                                                           │
│  PostgreSQL (transactional)  ·  Redis (locking, caching, counters)   │
│  Elasticsearch (search/dedup)  ·  S3/Blob (KYC documents)           │
│  ClickHouse / Redshift (analytics)                                    │
└──────────────────────────────────────────────────────────────────────┘
```

### Domain Aggregates (from DDD.xlsx)

| Aggregate Root | Domain | Bounded Context |
| --- | --- | --- |
| Lead | Origination | Lead Management |
| Prospect | Origination | Prospect Management |
| ApplicationCase | Origination | Credit Origination |
| Contract | Origination | Contract |
| Customer | Customer | Customer Master |
| Product | Product & Pricing | Product Catalog |
| Asset | Asset Catalog | Asset Registry |
| Organisation / Branch | Organisation | Hierarchy Master |

---

## 3. Infrastructure Components

### 3.1 Compute & Container Platform

| Component | Technology | Purpose |
| --- | --- | --- |
| Container Orchestration | Kubernetes (AKS/EKS) | Host all microservices; auto-scale |
| Service Mesh | Istio / Linkerd | mTLS between services, traffic shaping |
| Container Registry | Azure Container Registry / ECR | Versioned Docker images |
| Serverless (selective) | Azure Functions / AWS Lambda | Async jobs: temperature degradation, SLA timer evaluation, SMS dispatch |

### 3.2 API Gateway & Security

| Component | Technology | Purpose |
| --- | --- | --- |
| API Gateway | Kong / Azure APIM | Rate limiting, routing, auth token validation, channel tagging |
| Identity Provider | Azure AD B2C / Keycloak | OAuth 2.0 + OIDC; user login, device registration |
| WAF | Azure Front Door WAF / AWS WAF | Block injection, XSS, DDoS |
| Secrets Management | Azure Key Vault / AWS Secrets Manager | API keys, connection strings, service credentials |

### 3.3 Databases

| Store | Technology | Usage |
| --- | --- | --- |
| Transactional DB | PostgreSQL 15+ (HA pair, read replicas) | Lead, Prospect, Application, Audit, Exception Queue records |
| Distributed Cache & Lock | Redis Cluster | LRN/Prospect ID atomic generation lock (15 s timeout); session cache; attempt counters |
| Search & Dedup Index | Elasticsearch | Fast PAN/GSTIN/Mobile/Email deduplication; DedupLabel resolution |
| Document Store | Azure Blob Storage / S3 | KYC document images; Excel upload files; quote PDFs |
| Analytics / Reporting | ClickHouse or Amazon Redshift | Dashboards, KPI computation, aging reports |

### 3.4 Messaging & Events

| Component | Technology | Purpose |
| --- | --- | --- |
| Event Broker | Apache Kafka | Domain events: LeadCreated, LeadPromoted, ProspectValidated, ApplicationSanctioned, CustomerCreated |
| Message Queue (async tasks) | RabbitMQ / SQS | SMS dispatch, email notifications, DMS upload, fraud screening calls (non-blocking) |
| Scheduler | Quartz / Kubernetes CronJobs | Temperature degradation (daily), SLA breach check (hourly), aged-lead-block evaluation |

### 3.5 Observability Stack

| Component | Technology | Purpose |
| --- | --- | --- |
| Distributed Tracing | Jaeger / Zipkin / Azure Monitor | End-to-end request tracing across microservices |
| Log Aggregation | ELK Stack / Azure Log Analytics | Centralised structured logs per service |
| Metrics & Alerting | Prometheus + Grafana / Azure Monitor | Service health, DB latency, SLA breach rates, exception queue depth |
| Error Tracking | Sentry | Application error monitoring with context |

### 3.6 CI/CD & DevOps

| Component | Technology | Purpose |
| --- | --- | --- |
| Source Control | GitHub / Azure DevOps | Branching strategy (GitFlow); PR-gated merges |
| CI Pipeline | GitHub Actions / Azure Pipelines | Build, lint, unit test, SAST scan on every PR |
| CD Pipeline | ArgoCD (GitOps) | Automated deploy to Dev → QA → UAT → Production |
| Infrastructure as Code | Terraform + Helm charts | Reproducible cloud infrastructure |
| Environment Strategy | Dev · QA · UAT · Pre-Prod · Production | Mirrors DMS environment indicator: T → B → L |

### 3.7 Security & Compliance Infrastructure

| Component | Purpose |
| --- | --- |
| Column-level encryption | PAN, Aadhaar, GSTIN, Passport stored encrypted at rest (AES-256) |
| Audit Log Store | Append-only immutable audit table (PostgreSQL with pg_audit); all state changes captured with operator, timestamp, before/after values |
| Data Masking | PAN/Aadhaar displayed masked in all UI views per data protection policy |
| RBAC Engine | Role-based access control enforced at API gateway + service layer; hierarchy-aware (user sees only Leads/Prospects in their tree) |
| Network | Private VNet/VPC; services not publicly exposed; external integrations via private link or IP allowlist |

---

## 4. Phase 1 — Foundation & Core Lead Management

**Duration:** 10 weeks  
**Goal:** Authenticated users can create, view, assign, and interact with leads from mobile and desktop. Exception queue is operational.

### 4.1 Infrastructure Setup (Weeks 1–2)

- Provision cloud environment (Dev + QA).
- Deploy Kubernetes cluster; configure namespaces per service.
- Set up PostgreSQL (HA), Redis cluster, Kafka, API Gateway.
- Configure Identity Provider (Azure AD B2C / Keycloak); integrate with user/device registry.
- Establish CI/CD pipelines; configure SAST (Checkmarx / SonarQube).
- Set up Terraform state management and Helm chart templates.
- Configure observability stack (ELK, Prometheus, Grafana).

### 4.2 Authentication Service — LP1 (Weeks 2–3)

| Deliverable | Detail |
| --- | --- |
| User authentication | Verify active user against employee registry (error GL461 on failure) |
| Device validation | Primary IMEI → secondary fallback; legacy-compatibility mode configurable |
| GPS capture | Extract and store latitude/longitude when GPS-enabled flag is active |
| Session management | JWT issuance; token refresh; session revocation |
| Channel tag | Tag each request: Mobile / Desktop / API / Bulk |

**Business rules:** LP1.1, LP1.2, LP1.3, LP1.4

### 4.3 Lead Service — Base Lead Creation — LP2 (Weeks 3–5)

| Deliverable | Detail |
| --- | --- |
| Lead creation form (manual) | Mobile + Desktop channel; trimming, Lead Type capture |
| Source capture | Source Category (Internal/External/Campaign/Dealer) + Source Name — both mandatory |
| Minimum identifier validation | Name + one of Mobile/Email/PAN/GSTIN/Address; failure → Exception Queue |
| Applicant normalisation | Gender normalisation (Male → M, others → F; cleared for Non-Individual); co-applicant sequential labelling |
| DAN / PAN exemption | Validate DAN existence and uniqueness; errors LN4468, LN4470 |
| LRN generation | Redis-backed atomic increment with 15 s distributed lock; format: LOB-YYYYMM-NNNNNN; error D283 on lock timeout |
| Temp Customer Number | Format: TMP-YYYY-NNNNNN; annual reset controlled by system parameter |
| Default assignment | Auto-assign to CPU unless self-assign selected (role-gated) |
| Geotag storage | Latitude/longitude stored if mobile + GPS active |
| Geographic validation | Pincode-Branch distance check; mobile: warning LN3955 (save-allowed = Y); other channels: hard error |

**Business rules:** LP2.1–LP2.8

### 4.4 Assignment Service — LP5 (Weeks 5–6)

| Deliverable | Detail |
| --- | --- |
| 4-level hierarchy model | CPU → Branch Manager → Field Officer / Call Centre Associate |
| Assignment API | Capture assigned-to user, reason code, remarks, timestamp, previous owner |
| Immutable audit log | Assignment history written to append-only audit table; no edit/delete |
| FO absence escalation | Auto-escalate to Branch Manager when FO absence flag is active |
| Branch-change exception flow | Exception workflow with mandatory approval + audit trail |

**Business rules:** LP5.1–LP5.5

### 4.5 Interaction Service — LP6 (Weeks 6–7)

| Deliverable | Detail |
| --- | --- |
| Interaction logging | Type, timestamp, outcome, contact person, mode — all captured |
| Next action scheduling | Mandatory for In-Progress leads: date/time + mode |
| Attempt counters | System-maintained per lead: calls, messages, emails (stored in Redis, periodically flushed to DB) |
| Immutable history | No edit/delete; created-by + created-at stamp on every record |
| SMS dispatch | Async via message queue; failure logged silently (LP6.4); no transaction rollback |

**Business rules:** LP6.1–LP6.5

### 4.6 Exception Queue Service (Weeks 7–8)

| Deliverable | Detail |
| --- | --- |
| Exception record creation | Reason code, field(s) in error, source identifier, CPU ownership, cure SLA deadline |
| Queue management UI | CPU view: open exceptions, aged exceptions, resolution TAT |
| Cure SLA tracking | Configurable threshold (default 72 hrs); breach notification to CPU |

### 4.7 Lead Status Model (Week 8)

Implement status transitions: New → Assigned → In-Progress → Promoted / Closed. Enforce all state-machine guards in the Lead Service.

### 4.8 Role-Based Dashboards — Phase 1 Subset (Weeks 9–10)

| Dashboard | Audience |
| --- | --- |
| My To-Do List | Field Officers / Associates |
| Lead Pipeline (basic) | Branch Manager / CPU |
| Exception Queue | CPU / Operations |

### Phase 1 Exit Criteria

- Users can authenticate from mobile and desktop.
- Lead creation (manual) works across all channels with correct LRN generation.
- Assignment hierarchy (4 levels) operational with immutable audit log.
- Interaction logging with next action scheduling and attempt counters.
- Exception queue receives invalid records; CPU can manage and resolve.
- All Phase 1 business rules (LP1–LP2, LP5–LP6) covered by automated integration tests.

---

## 5. Phase 2 — Lead Intelligence & Bulk Ingestion

**Duration:** 8 weeks  
**Goal:** Bulk and API ingestion operational; identity verification and deduplication active; lead temperature engine and SLA escalation running.

### 5.1 Bulk Upload & API Ingestion — LP3 (Weeks 1–2)

| Deliverable | Detail |
| --- | --- |
| Excel upload service | Parse approved template; validate each row (Name + identifier + Source Category); valid → LRN; invalid → Exception Queue with reason code |
| API ingestion endpoint | Authenticated; schema validation; same LRN generation logic; exception routing |
| Batch processing | Chunked processing (configurable batch size) with resumable uploads |
| Bulk assignment | All ingested leads default to CPU; no unassigned records |

**Business rules:** LP3.1–LP3.4

### 5.2 Identity Capture & Deduplication — LP4 (Weeks 2–4)

| Deliverable | Detail |
| --- | --- |
| KYC format validation | PAN (AAAAA9999A), Aadhaar (Verhoeff + 12 digits), Passport, Voter ID format checks |
| Verhoeff algorithm | Implement digit reversal + permutation/multiplication table check |
| Caution list integration | External caution list service call on PAN; status ≠ Allowed → error LN5337 |
| UCIC mapping | External UCIC service call; map existing customer codes; assign DedupLabel |
| External PAN dedup API | COM110 → hard reject; COM111 → warning + halt; COM66 → soft warning |
| Internal dedup engine | Elasticsearch-backed: check against existing Lead, Prospect, Application records by Mobile/Email/PAN/GSTIN |
| DedupLabel resolution | PossibleExisting / New / Unknown / Conflict; Conflict routes to Exception Queue |
| Risk category check | Existing customers with restricted risk category → block and raise error |
| Effectively Filed check | Check for existing filed prospect/enquiry on same KYC identifiers; set flag |
| Gender/Occupation check | Male + HOUSE WIFE occupation → error COM122 |
| PAN mandatory check | Individual without DAN exemption and no PAN → error COM129 |

**Business rules:** LP4.1–LP4.9

### 5.3 Lead Temperature Engine — LP7 (Weeks 4–5)

| Deliverable | Detail |
| --- | --- |
| Temperature classification | Hot / Warm / Cold scoring from attempt counters, interaction outcomes, data completeness, recency |
| Scheduled degradation | Kubernetes CronJob: Hot→Warm (7 days inactivity), Warm→Cold (14 days); configurable thresholds; system-generated audit entry on each degradation |
| Manual override | User selects override temperature; StatusReason (code + free text) mandatory; StatusSuggestedBy = 'Manual' |
| Closure suggestion | N unanswered attempts or negative outcome triggers closure suggestion; ClosureReason code mandatory |

**Business rules:** LP7.1–LP7.4

### 5.4 SLA Engine & Escalation — LP5 (enhancement) (Weeks 5–6)

| SLA | Threshold | Escalation |
| --- | --- | --- |
| CPU First Assignment | 4 hours | Operations Officer |
| Branch-to-Officer Assignment | 24 hours | RVP |
| First Interaction after Assignment | 48 hours | Branch Manager |
| Exception Cure | 72 hours (configurable) | CPU / Central Team |
| FO Absence | Immediate | Branch Manager |

- Hourly Kubernetes CronJob evaluates all open SLA timers.
- Breaches publish `SLABreached` Kafka event → Notification Service → in-app + email alert.
- All breaches logged to audit table; surfaced on SLA Compliance dashboard.

### 5.5 Geographic Validation Service (Week 6)

| Deliverable | Detail |
| --- | --- |
| Pincode-Branch mapping table | Branch service area configuration with distance threshold; override table |
| Distance computation | Haversine formula between applicant pincode centroid and branch coordinates |
| Bypass rules | SME branches (segment code 'SM'); users with bypass access right; explicit exclusion list |
| Channel differentiation | Mobile: warning LN3955 (save-allowed = Y); All other channels: hard error |

### 5.6 Lead Qualification & Closure — LP8 Pre-conditions (Week 7)

| Deliverable | Detail |
| --- | --- |
| DedupLabel resolution gate | Block promotion if DedupLabel = Conflict |
| Pre-promotion validation | PAN (individual) / GSTIN (commercial) mandatory; format validation; caution list |
| Deceased lessee check | Customer record flag check; error "This Customer is deceased" |
| Active lease block | System parameter HP-ACTIVE-LEASE; block with error LN4323 equivalent if enabled |
| NRI eligibility | Passport validity ≥ 90 days; alternate mobile + email mandatory; product restrictions enforced |
| Asset validations | Cost > 0 (LN3574); cost ≥ finance amount; model year range (HP024 parameter) |
| Applicant record checks | Location name not null (LN5078); address lines 3–40 chars (LN5226/LN5227) |
| Lead closure | ClosureReason mandatory; record locked; interaction history frozen |

**Business rules:** LP8.1–LP8.9

### 5.7 Enhanced Dashboards (Week 8)

Add to Phase 1 dashboards:
- Lead temperature distribution.
- SLA compliance tracker.
- Aging report (0–7 / 8–15 / 16–30 / 30+ days buckets).
- Marketing source report (leads by Source Category/Name).

### Phase 2 Exit Criteria

- Bulk Excel and API ingestion end-to-end functional.
- Deduplication correctly assigns DedupLabel; UCIC mapping works.
- Temperature engine runs on schedule; manual override with audit captured.
- SLA timers trigger escalations on breach.
- Geographic validation active for both mobile (warning) and desktop (hard error) channels.
- All Phase 2 business rules covered by integration tests against real Redis and Elasticsearch instances.

---

## 6. Phase 3 — Prospect Creation & External Validation

**Duration:** 8 weeks  
**Goal:** Qualified leads promote to Prospects with validated legal identity; Prospect lifecycle (Draft → Validated → Active) fully operational.

### 6.1 Lead-to-Prospect Promotion Service — LP8 (Weeks 1–2)

| Deliverable | Detail |
| --- | --- |
| Full pre-promotion checklist | Execute all 17 validation checks listed in LP8 (see FS Section 5, LP8) |
| Prospect record creation | Draft status on creation; Lead status → Promoted |
| Prospect ID generation | Lock-protected atomic increment (Redis); format: PR-YYYY-NNNNNN |
| UCIC mapping | If PossibleExisting: map Prospect to existing UCIC + customer codes |
| Marketing employee notification | Dispatch EQ001/EQ002/EQ003 if creator ≠ marketing employee |
| Lineage link | Lead LRN → Prospect ID written to lineage table |

### 6.2 PAN/GSTIN Validation Integration — PP3 (Weeks 2–4)

| Deliverable | Detail |
| --- | --- |
| External PAN validation adapter | Synchronous call; on success: populate Legal Name + Registered Address from validated source |
| External GSTIN validation adapter | On success: populate Legal Entity Name + Registered Address; derive PAN from chars 3–12 |
| Validation metadata | Store source, timestamp, validation reference against Prospect record |
| Exception routing | Validation failure → exception queue with mandatory reason code; Prospect stays Draft; Central Team notified; 24-hour SLA |
| Manual override | Mandatory reason code + operator audit trail for any override of validated legal name/address |
| Prospect ID gating | Prospect ID generated only after validation success (PP3.1) |
| Geographic validation | Lessee registered pincode validated against branch; SME exempt; mobile warning / desktop hard error (LN3955) |
| Active branch check | Branch must have open business and accounting dates (error LN480) |

**Business rules:** PP3.1–PP3.6

### 6.3 Prospect Assignment Workflow — PP2 (Week 4)

| Deliverable | Detail |
| --- | --- |
| Assignment API | CPU → Manager → Associate; capture remarks, reason code, timestamp |
| Re-assignment | Manager re-assigns within branch or returns to CPU; Associate requests re-assignment |
| Branch change exception | Exception workflow with Central Team / senior manager approval |
| Immutable audit | All assignment actions → append-only audit log |

**Business rules:** PP2.1–PP2.3

### 6.4 Prospect Interface — PP1 (Weeks 4–5)

| Deliverable | Detail |
| --- | --- |
| Prospect view | Lead lineage, interaction history, DedupLabel, UCIC, temperature history |
| PAN/GSTIN masking | Masked display per data protection policy |
| RBAC filtering | Users see only Prospects within their assignment/reporting tree |
| Existing customer fast-track | Allow starting at Prospect stage directly when policy permits |

**Business rules:** PP1.1, PP1.2

### 6.5 Meeting & Interaction Management — PP4 (Weeks 5–6)

| Deliverable | Detail |
| --- | --- |
| Meeting logging | Meeting ID (MTG-YYYY-NNNNN), date/time (mandatory), attendees, notes (mandatory), mode, outcome |
| Reminders | Optional calendar integration; configurable: 1 day / 1 hour before; in-app notification fallback |
| Multiple meetings per Prospect | Unlimited; each immutable once logged; amendments recorded as new entries with original meeting ID reference |
| RBAC on meetings | Associate: own; Manager: team's |

**Business rules:** PP4.1–PP4.3

### 6.6 Prospect Status Model (Week 6)

Implement and enforce: Draft → Validated → Active → InAppraisal → CustomerCreated / Closed.

### 6.7 Dashboards — Phase 3 Additions (Weeks 7–8)

| Dashboard | Audience |
| --- | --- |
| Prospect Pipeline | Branch Manager / CPU / Central Team |
| Exception Queue (Prospect Validation) | CPU / Operations |
| Performance Dashboard (Lead→Prospect conversion) | RVP / Regional / Branch Heads |

### Phase 3 Exit Criteria

- Lead promotion with full 17-point checklist runs correctly; Prospect ID generated only post-validation.
- PAN and GSTIN validation adapters integrated with real external services (or stubs under contract).
- Prospect lifecycle (Draft → Validated → Active) enforced with correct state guards.
- Meeting management operational with immutable logging.
- RBAC correct at all Prospect views.

---

## 7. Phase 4 — Opportunity, Quote & Application Origination

**Duration:** 10 weeks  
**Goal:** Field associates can create opportunities, generate and lock quotes, initiate applications, upload KYC documents, and trigger parallel CAM and fraud screening.

### 7.1 Opportunity Service — PP5 (Weeks 1–2)

| Deliverable | Detail |
| --- | --- |
| Opportunity creation | Mandatory: Asset Category, Asset Class, LoB Tag; Opportunity ID: OPP-YYYY-NNNNNN |
| Multi-LoB support | One Opportunity per LoB; each tracked independently |
| Duplicate detection | Same Asset Category + Asset Class + LoB for same Prospect → configurable warn or block |
| Asset taxonomy master | Read-only reference service: Category / Class / Make / Model hierarchy |

**Business rules:** PP5.1–PP5.4

### 7.2 Quote Service — PP6 (Weeks 2–4)

| Deliverable | Detail |
| --- | --- |
| Product model price retrieval | Call Product Model Price Service; if NDLP ≠ submitted cost → override + advisory message |
| Rack rate quote | Fetch from Quote Master (taxonomy + lease type + pricing slabs); no approval required |
| Customised quote | Capture all pricing inputs; deviations from rack rate trigger manager/central team approval workflow |
| Approval workflow | Approval state machine with audit; unapproved quotes cannot be shared |
| Multiple quote versions | Version tracking per Opportunity; negotiation history maintained |
| Quote lock | Status: Draft → Shared → Locked; post-lock edits blocked without approved unlock |
| Unlock approval | Manager / Central Team approval required; unlock decisions auditable |
| Appraisal category validation | Validate against branch + lease type + asset class + market value + date (PP6.7) |

**Business rules:** PP6.1–PP6.7

### 7.3 Application Service — PP7 (Weeks 4–6)

| Deliverable | Detail |
| --- | --- |
| Application initiation | Application ID: APP-YYYY-NNNNNN; form pre-filled from Prospect data |
| Document checklist enforcement | Stage + asset class + lease type gated; missing mandatory docs block stage progression |
| KYC document upload | Compressed archive accepted; extracted per document type (Photo/Driving Licence/PAN/Passport/Other KYC); applicant prefix mapped (MA/A1/A2 → MAIN APPLICANT/ADDL-1/ADDL-2) |
| DMS integration | Route to Test (T) / Beta (B) / Live (L) endpoint from system parameter; document index returned |
| Status display at Prospect | Application ID, KYC status, CAM status visible on Prospect screen at all times |
| Sanction validity (existing customer) | Configurable sanction period; existing customers within validity → limit check only (bypass CAM) |
| Non-individual auto-eligibility | Single-stage approval branch: commercial lessee → auto-advance to Eligible For Application without fraud check |
| Modification controls | Block modification if: (a) lease contract or CAM in non-final state (LN3713); (b) active fraud investigation (LN3785) |
| Modification delete-and-re-insert | Delete/re-insert all applicant records except those with confirmed SMS or ANC status (consent chain protection) |

**Business rules:** PP7.1–PP7.12

### 7.4 Fraud Screening Integration — Hunter/Sherlock (Weeks 6–7)

| Deliverable | Detail |
| --- | --- |
| Post-save fraud check | Trigger Hunter + Sherlock async after application commit |
| Result evaluation | Both clear → fraud status = Clear; CIBIL request initiated |
| Non-clear result | Application → Pending status; fraud message logged; manual review triggered |
| Service inactive fallback | If fraud service inactive: call CIBIL directly for individual lessees |
| Non-blocking | Fraud check failure does not block application save |

**Business rules:** PP7.4, PP7.7, PP7.9, PP7.11

### 7.5 Credit Bureau (CIBIL) Integration (Week 7)

| Deliverable | Detail |
| --- | --- |
| CIBIL request | HTTP GET with parameterised request; 2xx = success |
| Non-2xx handling | Log indicator "Credit Bureau Request not Submitted"; transaction continues |
| Individual-only | CIBIL called for individual lessees only |

**Business rules:** PP7.7

### 7.6 Post-Save Caution List Screening (Week 7)

After application + applicant records committed: screen all applicants against internal caution database. No hard error → second commit. Hard error → block second commit; log for Central Team review.

**Business rules:** PP7.11

### 7.7 Welcome Communication (Week 8)

For designated eligible lease types (configurable system parameter): dispatch email + SMS to lessee upon application reaching Eligible For Application status.

**Business rules:** PP7.12

### 7.8 CAM Parallel Workflow Integration (Weeks 8–9)

| Deliverable | Detail |
| --- | --- |
| CAM initiation | Initiate CAM in parallel with KYC; both processes non-blocking to each other |
| Status tracking | CAM statuses: Not Started / In Progress / Approved / Declined |
| Sanction ID | Display sanction ID and package on Prospect screen once issued |

### 7.9 Enhanced Dashboards (Week 10)

| Dashboard | Additions |
| --- | --- |
| Prospect Pipeline | Opportunities open/quoted/locked; applications in appraisal |
| SLA Compliance | Quote turnaround; application initiation to appraisal TAT |
| Performance | Quote lock rate; CAM approval rate |

### Phase 4 Exit Criteria

- Opportunities created per LoB; duplicate detection functional.
- Rack rate and customised quote workflows end-to-end.
- Quote lock enforced; unlock requires approval.
- Application created with DMS document upload to correct environment.
- Fraud screening (Hunter/Sherlock) and CIBIL calls integrate; failures non-blocking.
- KYC and CAM statuses visible at Prospect screen.
- Post-save caution screening operational.
- All PP5–PP7 business rules covered by integration tests.

---

## 8. Phase 5 — Customer Creation, Lineage & Downstream Integration

**Duration:** 6 weeks  
**Goal:** Enterprise customer created on KYC+CAM gate; immutable lineage established; downstream systems triggered; full analytics and reporting live.

### 8.1 Customer Creation Gate — PP8 (Weeks 1–2)

| Deliverable | Detail |
| --- | --- |
| KYC + CAM gate | Hard gate: KYC = Complete AND CAM = Approved (both simultaneously) |
| Enterprise Customer ID | Format: CUST-YYYY-NNNNNN; generated on gate satisfaction |
| Customer master record | Populated from validated Prospect data |
| Customer creation blocked display | Clear UI indicator when gate not yet satisfied; status reason displayed |

**Business rules:** PP8.1

### 8.2 Lineage Service — PP8 (Weeks 2–3)

| Deliverable | Detail |
| --- | --- |
| Immutable lineage chain | Lead LRN → Prospect ID → Opportunity ID → Quote ID → Application ID → Enterprise Customer ID |
| Lineage store | Dedicated lineage table; append-only; no update/delete |
| Lineage API | Cross-reference query: given any ID in chain, return full lineage |
| Lineage display | Visible in Lead, Prospect, Application, and Customer screens |

**Business rules:** PP8.2

### 8.3 Role Assignment — PP8 (Week 3)

| Deliverable | Detail |
| --- | --- |
| Role assignment module | Assign: Lessee (Individual/Corporate), Dealer/Vendor, Depositor |
| Checklist enforcement | Role-specific policy checklist must be complete before role activation |
| Audit | Role assignment captured with operator, timestamp, checklist completion status |

**Business rules:** PP8.3

### 8.4 Downstream System Triggers (Weeks 3–4)

| Trigger | Downstream System |
| --- | --- |
| CustomerCreated event (Kafka) | Lease Agreement/Contract generation service |
| CustomerCreated event | Billing and payment setup service |
| CustomerCreated event | Asset delivery coordination module |
| OpportunityWon event | CRM / reporting system update |
| LMS Update | Update Lead Management System record with Prospect ID (LP8.9) |

### 8.5 Full Analytics Platform (Weeks 4–5)

Populate ClickHouse / Redshift with event-sourced data; build all remaining dashboards and KPI reports:

| KPI / Report | Computation |
| --- | --- |
| Lead Aging | Days since creation; bucketed: 0–7 / 8–15 / 16–30 / 30+ |
| First-Contact TAT | Assignment timestamp → first interaction timestamp |
| Attempts per Lead | Sum of call/message/email counters per lead |
| Response Rate | % leads with ≥ 1 positive lessee response |
| Data Completeness Score | % optional fields completed across all leads in period |
| Lead-to-Prospect Conversion Rate | Promoted leads / Total created leads |
| Prospect-to-Customer Conversion Rate | CustomerCreated / Total Prospects |
| Closure Reason Distribution | Count + % by ClosureReason code |
| Exception Cure Rate | Resolved within SLA / Total exceptions |
| Quote Lock Rate | Opportunities reaching QuoteLocked / Total quoted |
| CAM Approval Rate | CAM Approved / CAM Submitted |

### 8.6 Final Dashboards (Week 5)

Complete all 8 role-based dashboards per Section 13.1 of the FS:
- My To-Do List
- Lead Pipeline
- Prospect Pipeline
- Exception Queue
- SLA Compliance
- Aging Report
- Performance Dashboard
- Marketing Source Report

### 8.7 Production Readiness & Hardening (Week 6)

- Load testing: simulate 10× peak daily lead volume.
- Penetration testing: OWASP top 10; PAN/Aadhaar encryption verification.
- Disaster recovery drill: RDS failover; Redis failover; Kafka consumer group rebalancing.
- Run book documentation for all on-call scenarios.
- UAT sign-off with stakeholders (CPU, Branch Manager, Field Officer, Operations Officer).
- Production environment provisioning; go-live checklist completion.

### Phase 5 Exit Criteria

- Customer creation gate (KYC + CAM) enforced; Enterprise Customer ID generated correctly.
- Full lineage chain (Lead → Customer) queryable and immutable.
- Downstream Kafka consumers (contract, billing, asset delivery) receiving events.
- All 8 dashboards and 11 KPIs populated with real data.
- Load test results within SLA thresholds.
- Production environment signed off.

---

## 9. Cross-Phase Concerns

### 9.1 Audit & Immutability (All Phases)

Every state change across Lead, Prospect, Application, Assignment, and Interaction records must produce an immutable audit entry capturing:

| Field | Detail |
| --- | --- |
| entity_type | Lead / Prospect / Opportunity / Quote / Application / Customer |
| entity_id | ID of the mutated record |
| action | Created / StatusChanged / Assigned / Overridden / Closed / Promoted |
| before_value | JSON snapshot of changed fields before mutation |
| after_value | JSON snapshot of changed fields after mutation |
| operator_id | Authenticated user ID |
| timestamp | UTC timestamp with timezone |
| channel | Mobile / Desktop / API / Bulk |

Implementation: PostgreSQL append-only audit table with `pg_audit`; no application-level DELETE or UPDATE permitted on audit rows.

### 9.2 Number Generation Locking (All Phases)

All sequentially generated IDs (LRN, Prospect ID, Opportunity ID, Application ID, Customer ID) use the same Redis-backed distributed lock pattern:
- Acquire lock with 15-second TTL.
- Read current counter.
- Increment atomically.
- Write new counter.
- Release lock.
- On timeout: return error D283 to caller.

### 9.3 Notification Architecture

All notifications (SMS, email, in-app) flow through the Notification Service via async message queue. Failures are logged; they never roll back the triggering transaction. Notification types:
- EQ001/EQ002/EQ003 — marketing employee change alerts
- SLA breach alerts
- FO absence alerts
- Meeting reminders
- Welcome communication (eligible lease applications)
- Validation exception alerts (Central Team)

### 9.4 Environment Management

System parameter `ENV_INDICATOR` controls environment-specific behaviour:
- `T` — Test DMS endpoint
- `B` — Beta DMS endpoint
- `L` — Live (Production) DMS endpoint

All external service URLs, credentials, and thresholds are stored in Azure Key Vault / AWS Secrets Manager and injected at runtime via Kubernetes Secrets.

### 9.5 Configurable System Parameters

| Parameter | Usage |
| --- | --- |
| HP024 | Asset model year range for asset category 1 |
| ACTIVE-LEASE-BLOCK | Enable/disable active lease block rule |
| NRI-PASSPORT-DAYS | Minimum passport validity days (default 90) |
| TEMP-HOT-WARN-DAYS | Days of inactivity before Hot → Warm (default 7) |
| TEMP-WARM-COLD-DAYS | Days of inactivity before Warm → Cold (default 14) |
| EXCEPTION-CURE-SLA-HRS | Exception queue cure SLA in hours (default 72) |
| CPU-ASSIGN-SLA-HRS | CPU first assignment SLA (default 4) |
| BRANCH-OFFICER-SLA-HRS | Branch-to-officer assignment SLA (default 24) |
| FO-FIRST-INTERACTION-HRS | First interaction after assignment SLA (default 48) |
| SANCTION-VALIDITY-DAYS | Sanction period for existing customers |
| ANNUAL-RESET-LRN | Enable/disable annual LRN counter reset |
| ELIGIBLE-LEASE-TYPES | Comma-separated lease types triggering welcome communication |
| FRAUD-SERVICE-ACTIVE | Enable/disable Hunter/Sherlock screening |

---

## 10. Dependency Map

```
Phase 1  ─────────────────────────────────────────────────────────────
  [Infra Setup] → [Auth/Device Service]
  [Auth/Device Service] → [Lead Creation Service]
  [Lead Creation Service] → [Assignment Service]
  [Lead Creation Service] → [Exception Queue Service]
  [Assignment Service] → [Interaction Service]
  [Interaction Service] → [Lead Status Engine]

Phase 2  ─────────────────────────────────────────────────────────────
  [Phase 1 Complete] → [Bulk Upload/API Ingestion]
  [Phase 1 Complete] → [Identity & Dedup Service]
  [Identity & Dedup Service] requires: [Elasticsearch cluster, UCIC API, PAN Dedup API, Caution List API]
  [Identity & Dedup Service] → [Temperature Engine]
  [Assignment Service (P1)] → [SLA Engine] → [Notification Service]
  [Pincode-Branch Mapping] → [Geographic Validation]

Phase 3  ─────────────────────────────────────────────────────────────
  [Phase 2 Complete] → [Lead-to-Prospect Promotion]
  [Lead-to-Prospect Promotion] → [PAN Validation Adapter]
  [Lead-to-Prospect Promotion] → [GSTIN Validation Adapter]
  [PAN/GSTIN Validation] → [Prospect ID Generation]
  [Prospect ID Generation] → [Prospect Assignment Service]
  [Prospect Assignment Service] → [Meeting Service]

Phase 4  ─────────────────────────────────────────────────────────────
  [Phase 3 Complete] → [Opportunity Service]
  [Opportunity Service] → [Quote Service]
  [Quote Service] requires: [Product Price Service, Quote Master]
  [Quote Service] → [Application Service]
  [Application Service] requires: [DMS Integration, Document Checklist Config]
  [Application Service] → [KYC Workflow]
  [Application Service] → [CAM Workflow]
  [Application Service] → [Fraud Screening Adapter (Hunter/Sherlock)]
  [Fraud Screening] → [CIBIL Adapter]

Phase 5  ─────────────────────────────────────────────────────────────
  [Phase 4 Complete] → [Customer Creation Gate Service]
  [Customer Creation] → [Lineage Service]
  [Customer Creation] → [Role Assignment Service]
  [CustomerCreated Kafka Event] → [Contract Service, Billing Service, Asset Delivery]
  [All Phases] → [Analytics Platform]
```

---

## 11. Risk Register

| # | Risk | Impact | Likelihood | Mitigation |
| --- | --- | --- | --- | --- |
| R1 | External PAN/GSTIN validation service unavailable | High — Prospect creation blocked | Medium | Async retry queue; circuit breaker; manual override with mandatory audit |
| R2 | Redis distributed lock contention at high LRN generation load | Medium — error D283 returned to users | Low-Medium | Redis cluster with read replicas; retry with exponential backoff in client; monitor lock wait times |
| R3 | External dedup API (PAN COM110/111) response latency | High — lead creation blocked at peak | Medium | Async dedup with provisional lead creation; synchronous gate only at Prospect promotion |
| R4 | DMS unavailable during KYC upload | High — application initiation blocked | Low | Retry queue with exponential backoff; DMS health check before upload attempt |
| R5 | Hunter/Sherlock fraud services return unexpected schemas | Medium — fraud status miscategorised | Low | Strict adapter contract tests; schema versioning; default to Pending on parse error |
| R6 | Data volume growth exceeding PostgreSQL single-node capacity | High — DB bottleneck | Medium | Read replicas from Phase 1; partition large tables (audit, interaction) by month; archive strategy from Phase 3 |
| R7 | Regulatory change to PAN/Aadhaar validation rules | High — compliance breach | Low-Medium | Adapter pattern for all KYC validation; rules in system parameters; fast-deploy path for rule updates |
| R8 | Excel bulk upload template changes breaking parser | Medium — bulk ingestion fails | Low | Template version pinning; parser accepts multiple versions; validation error returns row-level detail |
| R9 | Third-party CIBIL API pricing / quota limits hit | Low-Medium — bureau requests throttled | Low | Queuing and rate-limit-aware retry; cache bureau results per applicant within configurable TTL |
| R10 | RBAC misconfiguration exposing cross-branch data | High — compliance and trust breach | Low | Automated RBAC regression tests on every deploy; deny-by-default policy; quarterly access review |
