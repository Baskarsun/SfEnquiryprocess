# UI Implementation Plan — Sundaram Finance Leasing LOS
## Loan Enquiry Generation & Full Origination Dashboard

| Document Control | |
|---|---|
| Version | 1.0 |
| Date | 2026-05-11 |
| Status | Draft |
| Reference | Enterprise Architecture.md, Implementation_Plan.md, EnquiryProcess/* |

---

## Table of Contents

1. UI Inventory & Screen Analysis
2. Technology Stack (EA-Aligned)
3. Frontend Architecture
4. Screen-by-Screen API Mapping
5. Component Library Design System
6. State Management Strategy
7. Authentication & Security Integration
8. Integration Layer (Kong → Dapr)
9. Phased Delivery Roadmap
10. Architecture Gap Register
11. Testing Strategy

---

## 1. UI Inventory & Screen Analysis

The UI prototype (`docs/EnquiryProcess/`) contains two distinct application surfaces:

### 1A. Loan Enquiry Generation — Field Officer App

A 7-step wizard (`sf_app.jsx`, `sf_screens_1.jsx`, `sf_screens_2.jsx`, `sf_shared.jsx`) for field officers to create loan enquiries from mobile and desktop.

| Step | Screen | Component | Purpose | Primary API |
|---|---|---|---|---|
| 0 | Login | `LoginScreen` | Employee ID + Device IMEI authentication (LP1) | `POST /auth/token` (Keycloak) |
| 1 | Contract Details | `ContractForm` | Asset, loan, and dealer information (LP2) | `GET /api/v1/reference/asset-taxonomy` |
| 2 | Applicant Details | `ApplicantForm` | Main + up to 3 co-applicants; KYC identifiers, contact, address | Inline validation only at this step |
| 3 | KYC Documents | `KYCUpload` | Drag-and-drop upload; document type auto-detection | Staged; uploads on submit |
| 4 | Review | `ReviewScreen` | Collapsible summary with Edit-step jump links | Read-only; no API calls |
| 5 | Processing | `ProcessingScreen` | 12-step animated progress (900ms–1500ms per step) | `POST /api/v1/leads` then DMS |
| 6 | Success / Error | `SuccessScreen` / `ErrorScreen` | Enquiry number display; fraud/CIBIL/KYC status tiles | `GET /api/v1/leads/{lrn}` |

**Form data model** (as prototyped):
```
{
  user:       { userId, imei, uuid, appVersion },
  contract:   { enquiryDate, contractType, assetClass, loanPurpose,
                assetMake, assetModel, assetType, fuelType, assetUsage, numUnits,
                assetCost, financeAmount, loanTenure, interestRate, netIncome, repaymentFreq,
                dealerCode, businessSource, appraisalCategory, assessmentCriteria,
                marketingEmployee, lmsLeadId, remarks },
  applicants: [{ type, name, dob, gender, constitution, pan, aadhaar, dl, passport,
                 passportExpiry, voterId, mobile, altMobile, email, smsConsent,
                 addrLine1, addrLine2, pincode, city, state, isNRI }],
  kyc:        { files: [{ id, name, size, docType, applicantType }] }
}
```

**Client-side validations already prototyped:**
- PAN: `/^[A-Z]{5}[0-9]{4}[A-Z]$/`
- Aadhaar: 12 digits (Verhoeff to be enforced server-side)
- Mobile: 10 digits
- Email: RFC-compliant regex
- Pincode: 6 digits
- Finance amount ≤ Asset cost (LTV indicator rendered in real time)
- NRI: passport + expiry mandatory; altMobile + email mandatory
- Asset cost > 0 (LN3574 rule)

**Responsive behaviour:** `useWindowWidth()` hook; mobile (`< 768px`) collapses sidebar to a sticky dot-progress top bar. Desktop shows 268 px fixed sidebar.

---

### 1B. Full Leasing LOS — Manager/Operations Dashboard

A multi-page SPA (`docs/EnquiryProcess/components/`) with a persistent sidebar shell exposing all 6 origination stages.

| Page | Component | Role Access | Purpose |
|---|---|---|---|
| Dashboard | `dashboard.jsx` | All roles | KPI cards (6), funnel chart, alerts, recent leads table |
| Lead Generation | `leads.jsx` | FO, BM, CPU | Lead list with filter/sort; inline lead creation trigger |
| Enquiry / Prospect | `prospects.jsx` | BM, CPU, AML Officer | Prospect cards: PAN status, AML flag, KYC status, CIBIL |
| Opportunity | `opportunities.jsx` | FO, BM | Opportunity list; create opportunity per LoB |
| Pricing & Quote | (implied) | Pricing Analyst, BM | Rack rate retrieval; customised quote; approval workflow |
| Application | `applications.jsx` | BM, CPU | Application origination; KYC + CAM parallel status |
| Customer Creation | (implied) | CPU, Central Team | Customer gate (KYC=COMPLETE + CAM=APPROVED); ECID |

**Role system** (prototyped in `data.jsx`):
```
SF_ROLES = [ Field Officer | Branch Manager | CPU / Central Team | Pricing Analyst | AML Officer ]
```

**KPI data model** (`SF_KPI`):
```
{ totalLeads, prospects, opportunities, quotesLocked, applications, customers,
  amlFlagged, pendingKyc }
```

**Pipeline funnel** shown in sidebar footer: Lead → Prospect → Opportunity → Quote → Application → Customer.

---

## 2. Technology Stack (EA-Aligned)

The Enterprise Architecture document mandates the following. The frontend must integrate with all listed components.

### 2.1 Prescribed Stack

| Layer | Technology | EA Reference | Notes |
|---|---|---|---|
| **Frontend Framework** | **React** (+ TypeScript) | Implementation_Plan §2 "Web App (React/Angular)" | Use React 18+ with strict TypeScript |
| **Build Toolchain** | **Vite** | EA: "Golden Path template" | Fast HMR; ESM output; replaces CRA |
| **Design Font** | **Plus Jakarta Sans** | Prototyped in `sf_shared.jsx` | Already Google Fonts CDN; self-host via `@fontsource` for on-prem |
| **State Management** | **Zustand** or **React Query + Context** | EA: API-first; CQRS read models | React Query for server state; Zustand for global UI state |
| **API Communication** | **REST over HTTPS** via Kong | EA §API Gateway: Kong/WSO2 | All calls go through Kong; JWT in `Authorization: Bearer` header |
| **Authentication** | **Keycloak** (OIDC / OAuth 2.0 + PKCE) | EA §Security: "OIDC/JWT at Kong" | `@react-keycloak/web` adapter; token auto-refresh |
| **API Client** | **Axios** with interceptors | EA: TLS 1.2+ north-south | Interceptor attaches JWT; handles 401 refresh; sets `X-Channel` header |
| **Routing** | **React Router v6** | SPA requirement | Route guards for RBAC; `<ProtectedRoute>` checks Keycloak roles |
| **File Upload** | Browser `FileReader` + multipart POST | EA §DMS Integration | Max 10 MB/file; JPEG/PNG/PDF; compress on client before upload |
| **i18n / Locale** | **react-i18next** | Multi-branch deployment | EN only initially; ₹ currency formatting via `Intl.NumberFormat('en-IN')` |
| **Observability** | **OpenTelemetry JS SDK** | EA §Observability: Jaeger | Trace frontend user interactions; propagate `traceparent` header to Kong |
| **Testing** | **Vitest + React Testing Library** | EA §Testability | Unit + component tests; contract tests via MSW |
| **E2E** | **Playwright** | EA §Chaos/E2E strategy | Run against staging-k8s-cluster |
| **Container** | **Nginx Alpine Docker image** | EA: on-prem K8s | Serve React SPA; Kong handles SSL termination |
| **CI/CD** | **GitLab CI → ArgoCD** | EA §GitOps | Build → lint → test → SAST → Docker push to Harbor → ArgoCD deploy |

### 2.2 Kong Integration Points

Every API call from the React SPA passes through Kong, which:
- Validates the JWT issued by Keycloak
- Extracts `sub` (user ID) and injects it as `X-User-Id` header to backend
- Enforces rate limiting (`NFR-P1`: <50ms p99)
- Tags the channel (`X-Channel: DESKTOP` or `MOBILE`)
- Routes to the correct microservice based on path prefix

```
React SPA → Kong (/api/v1/leads)      → lead-service
React SPA → Kong (/api/v1/prospects)  → prospect-service (Phase 3)
React SPA → Kong (/api/v1/dms/upload) → dms-adapter
React SPA → Kong (/api/v1/reference)  → reference-data-service
```

### 2.3 CRITICAL Architecture Gap: Quarkus vs Spring Boot

> **The existing `lead-service` is built on Quarkus (Hibernate Panache / RESTEasy Reactive), but the Enterprise Architecture mandates Spring Boot + Clean Architecture for all microservices.**

This gap must be resolved before the UI integrates with the backend. The frontend is framework-agnostic; the API contract (OpenAPI) is what matters. However, for EA compliance, the backend must be migrated or a decision must be made to approve Quarkus as an exception. See §10 Gap Register.

---

## 3. Frontend Architecture

### 3.1 Project Structure

```
sf-leasing-ui/
├── public/
├── src/
│   ├── app/
│   │   ├── App.tsx                   # Root router
│   │   ├── keycloak.ts               # Keycloak instance config
│   │   └── routes.tsx                # Route definitions + ProtectedRoute
│   ├── design-system/
│   │   ├── tokens.ts                 # SF colour tokens (from sf_shared.jsx)
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Select.tsx
│   │   ├── Card.tsx
│   │   ├── Alert.tsx
│   │   ├── Badge.tsx
│   │   └── index.ts
│   ├── features/
│   │   ├── enquiry/                  # Loan Enquiry Generation wizard
│   │   │   ├── EnquiryWizard.tsx     # Step router (steps 0–6)
│   │   │   ├── steps/
│   │   │   │   ├── LoginStep.tsx
│   │   │   │   ├── ContractStep.tsx
│   │   │   │   ├── ApplicantStep.tsx
│   │   │   │   ├── KYCStep.tsx
│   │   │   │   ├── ReviewStep.tsx
│   │   │   │   ├── ProcessingStep.tsx
│   │   │   │   └── SuccessStep.tsx
│   │   │   ├── hooks/
│   │   │   │   ├── useEnquiryForm.ts
│   │   │   │   └── useSubmitEnquiry.ts
│   │   │   └── types.ts
│   │   ├── dashboard/
│   │   │   ├── Dashboard.tsx
│   │   │   ├── KPICard.tsx
│   │   │   ├── PipelineFunnel.tsx
│   │   │   └── AlertPanel.tsx
│   │   ├── leads/
│   │   │   ├── LeadList.tsx
│   │   │   ├── LeadDetail.tsx
│   │   │   ├── CreateLeadDrawer.tsx
│   │   │   └── hooks/useLeads.ts
│   │   ├── prospects/
│   │   ├── opportunities/
│   │   ├── quotes/
│   │   ├── applications/
│   │   └── customers/
│   ├── shared/
│   │   ├── api/
│   │   │   ├── axios-client.ts       # Axios instance with Kong headers
│   │   │   ├── leads.api.ts
│   │   │   ├── prospects.api.ts
│   │   │   ├── reference.api.ts
│   │   │   └── dms.api.ts
│   │   ├── hooks/
│   │   │   ├── useWindowWidth.ts
│   │   │   └── useRBAC.ts
│   │   ├── layout/
│   │   │   ├── AppShell.tsx          # Sidebar + topbar (from shell.jsx)
│   │   │   └── Sidebar.tsx
│   │   └── types/
│   │       └── domain.ts             # Lead, Prospect, Applicant, etc.
│   └── main.tsx
├── Dockerfile
├── nginx.conf
├── vite.config.ts
└── k8s/
    ├── deployment.yaml
    ├── service.yaml
    └── kongingress.yaml
```

### 3.2 Routing

```tsx
// routes.tsx
/login              → KeycloakCallback (PKCE flow)
/enquiry            → EnquiryWizard (ProtectedRoute — any role)
/dashboard          → Dashboard (ProtectedRoute — any role)
/leads              → LeadList (ProtectedRoute — roles: FO, BM, CPU)
/leads/:lrn         → LeadDetail
/prospects          → ProspectList (ProtectedRoute — BM, CPU, AML)
/prospects/:id      → ProspectDetail
/opportunities/:id  → OpportunityDetail
/quotes/:id         → QuoteDetail
/applications/:id   → ApplicationDetail
```

### 3.3 API Client

```tsx
// axios-client.ts
const apiClient = axios.create({ baseURL: import.meta.env.VITE_KONG_BASE_URL });

apiClient.interceptors.request.use(config => {
  config.headers.Authorization = `Bearer ${keycloak.token}`;
  config.headers['X-Channel'] = isMobile() ? 'MOBILE' : 'DESKTOP';
  // traceparent injected by OTel SDK automatically
  return config;
});

apiClient.interceptors.response.use(null, async error => {
  if (error.response?.status === 401) {
    await keycloak.updateToken(30);  // refresh token
    return apiClient.request(error.config);
  }
  return Promise.reject(error);
});
```

---

## 4. Screen-by-Screen API Mapping

### 4.1 Login Screen (Step 0)

| Action | API | Headers | Notes |
|---|---|---|---|
| Authenticate user | Keycloak `/realms/sf-leasing/protocol/openid-connect/token` | PKCE flow | Kong validates JWT on all subsequent requests; `X-User-Id` extracted from `sub` claim |
| Device registration (IMEI) | `POST /api/v1/auth/device` (internal) | `X-Device-Id: {imei}` | Validate IMEI against employee device registry (LP1.2) |
| App version check | `GET /api/v1/auth/version` | — | Returns minimum supported version; warn if outdated |

**Error codes:**
- `GL461` — User not found in employee registry → display "Contact your branch administrator"
- Device mismatch → display secondary IMEI fallback prompt (LP1.2)

### 4.2 Contract Form (Step 1)

| Action | API | Notes |
|---|---|---|
| Load asset taxonomy | `GET /api/v1/reference/asset-taxonomy` | Returns `{ categories, classes, makes, models }` for dropdowns |
| Load dealer list | `GET /api/v1/reference/dealers?branchCode={b}` | Filtered by user's branch; used for dealer code autocomplete |
| Load appraisal categories | `GET /api/v1/reference/appraisal-categories` | Static reference data |
| Validate LMS Lead ID (optional) | `GET /api/v1/leads/by-lms/{lmsId}` | Check if LMS ID already linked (LP2) |

**All reference data fetched on mount; cached in React Query with `staleTime: 5 minutes`.**

### 4.3 Applicant Form (Step 2)

| Action | API | Notes |
|---|---|---|
| Pincode lookup | `GET /api/v1/reference/pincode/{pin}` | Returns `{ city, state, branchCode, distanceKm }` — auto-fills city/state; surface geographic warning if `distanceKm > threshold` (LN3955) |
| Validate DAN | `GET /api/v1/reference/dan/{dan}` | Check DAN existence and uniqueness (LP2: errors LN4468, LN4470) |

**Client-side only at this step:** PAN regex, Aadhaar 12-digit check, mobile 10-digit, email format, passport expiry ≥ 90 days for NRI (LP8 rule). Server-side Verhoeff check and caution list happen at step 5.

### 4.4 KYC Upload (Step 3)

| Action | API | Notes |
|---|---|---|
| No API calls at this step | — | Files held in `formData.kyc.files` (File objects in memory) |
| Client-side only | — | File type validation (JPEG/PNG/PDF), size check (< 10 MB), doc type inference from filename |

Files are uploaded to DMS in Step 5 (Processing) **after** the lead record is created and the LRN is obtained.

### 4.5 Review Screen (Step 4)

No API calls. Pure render of `formData` with edit-step jump links.

**Masking:** Aadhaar shown as `••••••••XXXX` in review (data protection policy). PAN shown as-is (not considered sensitive for display per current policy).

### 4.6 Processing Screen (Step 5) — Critical Integration

This screen orchestrates the full lead creation flow. The 12 animated steps map to these API calls:

| Animated Step | Backend Operation | API Endpoint | Sync / Async |
|---|---|---|---|
| 1. Authenticating user & device | JWT validation (Kong) | Already done by Kong on every request | Sync |
| 2. Validating input completeness | Client-side validation re-check | — | Client |
| 3. Resolving branch & employee record | `GET /api/v1/employees/{userId}/branch` | Internal call via Kong | Sync |
| 4. KYC de-duplication & caution check | Included in `POST /api/v1/leads` (LP4) | Lead creation triggers dedup internally | Sync |
| 5. Geographic / pincode validation | Included in `POST /api/v1/leads` (LP2.8) | Returns `warning: LN3955` if mobile channel | Sync |
| 6. Product model market value retrieval | Included in `POST /api/v1/leads` (LP2) | Backend calls Product Model Price Service | Sync |
| 7. Appraisal category validation | Included in `POST /api/v1/leads` | Backend validates against branch + lease type | Sync |
| 8. Creating enquiry record | **`POST /api/v1/leads`** | Main lead creation; returns `{ lrn, tmpCustomerNo, warnings[] }` | Sync |
| 9. Initiating fraud screening | `POST /api/v1/leads/{lrn}/fraud-check` | Async trigger; returns 202 Accepted | Async (fire-and-forget) |
| 10. Requesting CIBIL report | `POST /api/v1/leads/{lrn}/cibil-request` | Async trigger; returns 202 Accepted | Async (fire-and-forget) |
| 11. Uploading KYC documents to DMS | `POST /api/v1/dms/documents` (multipart) | Upload each file; requires LRN; returns `{ documentIndex }` | Async (parallel) |
| 12. Finalising & generating enquiry number | `GET /api/v1/leads/{lrn}` | Confirm record; return final LRN for display | Sync |

**Request body for `POST /api/v1/leads`:**
```typescript
interface CreateLeadRequest {
  sourceCategory: 'INTERNAL' | 'EXTERNAL' | 'CAMPAIGN' | 'DEALER';
  sourceName: string;                          // dealerCode or businessSource
  leadType: 'INDIVIDUAL' | 'NON_INDIVIDUAL';
  enquiryDate: string;                         // ISO date
  contractType: string;
  assetCategory: string;
  assetClass: string;
  assetMake: string;
  assetModel: string;
  assetType: string;
  assetCost: number;
  financeAmount: number;
  loanTenure: number;
  lmsLeadId?: string;
  remarks?: string;
  applicants: ApplicantRequest[];
}

interface ApplicantRequest {
  type: 'MAIN APPLICANT' | 'CO-APPLICANT 1' | 'CO-APPLICANT 2' | 'CO-APPLICANT 3';
  name: string;
  dob: string;
  gender?: 'M' | 'F' | 'OTHER';              // server normalises: Male→M; Female/others→F; blank for NON_INDIVIDUAL
  constitution: 'INDIVIDUAL' | 'NON_INDIVIDUAL';
  pan?: string;
  aadhaar?: string;
  drivingLicence?: string;
  voterId?: string;
  passport?: string;
  passportExpiry?: string;
  mobile: string;
  altMobile?: string;
  email?: string;
  smsConsent: boolean;
  addressLine1: string;
  addressLine2?: string;
  pincode: string;
  city: string;
  state: string;
  isNRI: boolean;
}
```

**Response from `POST /api/v1/leads`:**
```typescript
interface CreateLeadResponse {
  lrn: string;                                 // e.g. "LEA-202605-000042"
  tmpCustomerNo: string;                       // e.g. "TMP-2026-000007"
  status: 'CREATED' | 'EXCEPTION_QUEUE';
  routedToExceptionQueue: boolean;
  warnings: Array<{ code: string; message: string; }>;  // e.g. LN3955 (pincode), NDLP price update
  dedupLabels: Array<{ applicantIndex: number; label: 'New' | 'PossibleExisting' | 'Unknown' | 'Conflict'; }>;
}
```

### 4.7 Success Screen (Step 6)

| Action | API | Notes |
|---|---|---|
| Display enquiry number | From `CreateLeadResponse.lrn` | No additional API call needed |
| Poll fraud screening status (optional) | `GET /api/v1/leads/{lrn}/fraud-status` | Optional: 3-attempt polling at 5s intervals; show "Pending" if not resolved |
| Copy to clipboard | Browser `navigator.clipboard.writeText()` | No API |

---

## 5. Component Library Design System

### 5.1 Design Tokens (from `sf_shared.jsx`)

```typescript
export const SF = {
  navy:      '#1B3A6B',    // Primary brand colour — backgrounds, primary buttons
  navyDark:  '#0F2344',    // Sidebar gradient start
  navyLight: '#2A4F8F',    // Sidebar gradient end
  gold:      '#C8992B',    // Accent — active step, gold buttons, highlights
  goldLight: '#EDD69C',    // Step number backgrounds
  goldBg:    '#FBF6EC',    // Warning/advisory card backgrounds
  bg:        '#EEF2FA',    // Page background
  white:     '#FFFFFF',
  success:   '#0D7A4A',
  successBg: '#E6F6EF',
  error:     '#C0392B',
  errorBg:   '#FDECEA',
  warning:   '#D97706',
  info:      '#2563EB',
  infoBg:    '#EFF6FF',
  text:      '#111827',
  muted:     '#6B7280',
  border:    '#DDE3F0',
} as const;

// LOS Dashboard uses a darker palette
export const LOS = {
  navyDeep: '#002060',     // Sidebar background
  amber:    '#FFB81C',     // Logo mark, active nav highlight
};
```

### 5.2 Core Components (TypeScript)

All components from `sf_shared.jsx` and `shell.jsx` must be migrated to typed React components:

| Prototype Component | TypeScript Component | Props |
|---|---|---|
| `SFInput` | `<Input>` | `label, value, onChange, error?, required?, type?, prefix?, suffix?, maxLength?, helper?, readOnly?` |
| `SFSelect` | `<Select>` | `label, value, onChange, options, required?, error?, placeholder?` |
| `SFButton` | `<Button>` | `variant: 'primary'|'gold'|'outline'|'danger'|'ghost', size: 'sm'|'md'|'lg', loading?, disabled?, fullWidth?` |
| `SFCard` | `<Card>` | `title?, action?` |
| `SFAlert` | `<Alert>` | `type: 'info'|'success'|'error'|'warning'` |
| `SFBadge` | `<Badge>` | `color?` |
| `FormGrid` | `<FormGrid>` | `cols?: 2|3` |
| `NavButtons` | `<NavButtons>` | `onBack, onNext, backLabel?, nextLabel?, nextVariant?, disabled?, loading?` |
| `SFSidebar` | `<EnquirySidebar>` | `step, user, isMobile` |
| `AppShell` | `<AppShell>` | `page, setPage, role, setRole` |
| `SFLogo` | `<SFLogo>` | `white?, compact?` |

### 5.3 Feature-Specific Components

| Component | Purpose |
|---|---|
| `<ApplicantTabs>` | Tab bar for main + co-applicants; green tick on valid; remove button |
| `<LTVIndicator>` | Real-time LTV% chip below finance/cost fields |
| `<KYCDropzone>` | Drag-and-drop zone with file list and applicant assignment selector |
| `<ProcessingRing>` | SVG progress ring with percentage; animated step checklist |
| `<EnquiryNumberCard>` | Dark gradient card with gold LRN; copy button |
| `<PipelineFunnel>` | Horizontal funnel bars in sidebar; KPI grid on dashboard |
| `<LeadStatusBadge>` | Hot/Warm/Cold with correct colours and temperature icon |
| `<DedupLabelBadge>` | New/PossibleExisting/Conflict/Unknown with severity colours |

---

## 6. State Management Strategy

### 6.1 Enquiry Wizard State

The wizard uses **React `useState` + `localStorage` persistence** (as prototyped). In the production app:

- `useEnquiryForm()` custom hook owns the `formData` object
- Each step receives `formData` + `setFormData` as props (current prototype pattern is correct)
- Persist to `localStorage` on every `formData` change (step recovery on browser refresh)
- Clear `localStorage` on successful submission (`reset()`)

**Do not use a global store (Redux/Zustand) for wizard state** — it is self-contained and discarded after submission.

### 6.2 Server State (React Query)

```typescript
// Reference data (long cache)
useQuery(['assetTaxonomy'], fetchAssetTaxonomy, { staleTime: 30 * 60 * 1000 })
useQuery(['dealers', branchCode], () => fetchDealers(branchCode), { staleTime: 15 * 60 * 1000 })

// Lead list (short cache, paginated)
useInfiniteQuery(['leads', filters], ({ pageParam = 0 }) => fetchLeads(filters, pageParam))

// Lead creation mutation
useMutation(createLead, {
  onSuccess: (data) => {
    queryClient.invalidateQueries(['leads']);
    goToStep(6);
    setResult(data);
  },
  onError: (err) => {
    setError(err.message);
    goToStep(6); // Shows ErrorScreen
  }
})
```

### 6.3 Global App State (Zustand)

```typescript
interface AppStore {
  currentRole: Role;
  setRole: (role: Role) => void;
  currentPage: Page;
  setPage: (page: Page) => void;
  user: AuthUser | null;
  setUser: (user: AuthUser) => void;
}
```

---

## 7. Authentication & Security Integration

### 7.1 Keycloak PKCE Flow

```typescript
// keycloak.ts
const keycloak = new Keycloak({
  url:      import.meta.env.VITE_KEYCLOAK_URL,    // e.g. https://iam.sf-internal.com
  realm:    'sf-leasing',
  clientId: 'leasing-ui',
});

// main.tsx
const authenticated = await keycloak.init({
  onLoad: 'login-required',
  pkceMethod: 'S256',
  checkLoginIframe: false,
});
```

**JWT claims used:**
- `sub` → Employee ID (injected as `X-User-Id` by Kong)
- `realm_access.roles` → `['field_officer' | 'branch_manager' | 'cpu' | 'pricing_analyst' | 'aml_officer']`
- `branch_code` → Custom claim; used for branch-scoped filtering

### 7.2 Device Registration (LP1.2)

After Keycloak authentication, the app must register the device IMEI:
1. `navigator.mediaDevices.getUserMedia` is not available in browsers (IMEI is device-level, not web)
2. **For PWA/mobile**: Retrieve IMEI from a native wrapper (React Native or Capacitor) via a JavaScript bridge
3. **For desktop browser**: IMEI field is manually entered (as in the prototype); UUID auto-generated via `crypto.randomUUID()`
4. Device is validated server-side against the employee's registered device list

### 7.3 Data Masking in UI

| Field | Display Masking | Rule |
|---|---|---|
| Aadhaar | `••••••••XXXX` (last 4 visible) | Review screen + Prospect view |
| PAN | Shown as-is | Not masked per current policy |
| GSTIN | Shown as-is | Not masked |
| Mobile | Full display | No mask in current phase |

### 7.4 HTTPS / CORS

- All API calls go via Kong (single base URL)
- Kong handles CORS preflight; no CORS config needed in React
- React app served from Nginx inside K8s; Kong acts as reverse proxy
- `Content-Security-Policy` header set at Nginx level; disallows inline scripts except `nonce`

---

## 8. Integration Layer (Kong → Dapr → Microservices)

### 8.1 API Routes through Kong

```yaml
# kongingress.yaml
- path: /api/v1/leads
  service: lead-service.lead-ns.svc.cluster.local:8080
  plugins: [jwt-auth, rate-limiting, request-transformer]

- path: /api/v1/reference
  service: reference-service.ref-ns.svc.cluster.local:8080
  plugins: [jwt-auth, response-caching]

- path: /api/v1/dms
  service: dms-adapter.dms-ns.svc.cluster.local:8080
  plugins: [jwt-auth, rate-limiting]
```

### 8.2 Dapr Sidecar (East-West, not UI-visible)

The UI does **not** interact with Dapr directly — Dapr is purely internal (east-west). However, the UI must be aware of the **async event flow** it triggers:

```
UI POST /api/v1/leads
  → Kong → lead-service (via Dapr invoke if across namespaces)
      → lead-service publishes LeadCaptured event to Kafka
          → dedup-service subscribes (Dapr pub/sub)
          → notification-service subscribes (Dapr pub/sub)
          → compliance-engine subscribes (Dapr pub/sub)
```

The UI will poll for async results (fraud screening, CIBIL) via `GET /api/v1/leads/{lrn}/status`.

### 8.3 DMS Document Upload Flow

```
1. UI: POST /api/v1/dms/documents (multipart)
   Headers: Authorization, X-Lead-LRN, X-Applicant-Type (MA/A1/A2/A3), X-Doc-Type
   Body: file (binary)

2. DMS adapter receives → calls actual DMS endpoint (Test/Beta/Live per ENV_INDICATOR)

3. DMS returns: { documentIndex: string }

4. UI stores documentIndex in SuccessScreen display
```

**File upload URL format for Kong:**
```
POST https://api.sf-internal.com/api/v1/dms/documents
Content-Type: multipart/form-data
X-Lead-LRN: LEA-202605-000042
X-Applicant-Type: MA
X-Doc-Type: PAN_CARD
```

---

## 9. Phased Delivery Roadmap

Aligned with the backend phase plan. Frontend phases track backend readiness.

### Phase 1 (Weeks 1–10) — Enquiry Generation Wizard + Basic Lead List

**Deliverable:** Field Officers can authenticate, create loan enquiries, and see their lead pipeline.

| Week | Frontend Milestone | Depends On |
|---|---|---|
| 1–2 | Scaffold React+Vite project; configure GitLab CI; Keycloak integration; Nginx Dockerfile | Keycloak realm provisioned |
| 2–3 | Design system (all SF tokens → TypeScript components); Storybook | — |
| 3–5 | Enquiry Wizard steps 0–4 (Login → Review); all client-side validations; localStorage persistence | Reference data API ready |
| 5–6 | Processing screen + `POST /api/v1/leads` integration; DMS upload | `lead-service` `/api/v1/leads` ready |
| 6–7 | Success/Error screens; LRN display; copy-to-clipboard; pincode/NDLP advisory banners | — |
| 7–8 | AppShell + Lead List view; `GET /api/v1/leads` with pagination/filter | Lead search API ready |
| 8–9 | Exception Queue view for CPU users | Exception queue API ready |
| 9–10 | Mobile responsive QA; Playwright E2E test suite (happy path + error paths) | — |

**APIs consumed in Phase 1:**
- `POST /realms/sf-leasing/.../token` (Keycloak)
- `GET /api/v1/reference/asset-taxonomy`
- `GET /api/v1/reference/dealers?branchCode=`
- `GET /api/v1/reference/pincode/{pin}`
- `POST /api/v1/leads`
- `GET /api/v1/leads` (paginated)
- `GET /api/v1/leads/{lrn}`
- `GET /api/v1/exception-queue`
- `POST /api/v1/dms/documents`

---

### Phase 2 (Weeks 11–18) — Lead Intelligence & Bulk Dashboard

**Deliverable:** Bulk upload tracking, temperature indicators, SLA compliance dashboard.

| Feature | UI Component | New APIs |
|---|---|---|
| Lead temperature display | `<LeadTemperatureBadge>` Hot/Warm/Cold | `GET /api/v1/leads?temperature=` |
| Temperature manual override | Override modal with reason code | `PATCH /api/v1/leads/{lrn}/temperature` |
| SLA compliance panel | `<SLADashboard>` with breach indicators | `GET /api/v1/dashboard/sla-summary` |
| Bulk upload UI | `<BulkUploadDrawer>` Excel drop zone with row-level error report | `POST /api/v1/leads/bulk` |
| Aging report | `<AgingReport>` 0–7/8–15/16–30/30+ buckets | `GET /api/v1/dashboard/aging` |
| Marketing source report | Pie/bar chart by Source Category/Name | `GET /api/v1/dashboard/lead-sources` |

---

### Phase 3 (Weeks 19–26) — Prospect Module

**Deliverable:** Prospect creation, PAN/GSTIN validation status, meeting management, prospect pipeline.

| Feature | UI Component | New APIs |
|---|---|---|
| Prospect list with filters | `<ProspectList>` | `GET /api/v1/prospects` |
| Prospect detail view | `<ProspectDetail>` with lineage breadcrumb | `GET /api/v1/prospects/{id}` |
| PAN/GSTIN validation status | `<ValidationStatusCard>` | `GET /api/v1/prospects/{id}/kyc-status` |
| Lead lineage panel | `<LineageChain>` LRN → Prospect ID | `GET /api/v1/lineage/{id}` |
| Meeting log | `<MeetingLog>` with calendar reminder | `POST /api/v1/prospects/{id}/meetings` |
| Prospect assignment | Reassign modal with reason | `POST /api/v1/prospects/{id}/assign` |
| Prospect pipeline dashboard | Stage funnel + KPI | `GET /api/v1/dashboard/prospect-pipeline` |

---

### Phase 4 (Weeks 27–36) — Opportunity, Quote & Application

**Deliverable:** Full origination workflow from Opportunity creation to Application submission with KYC + CAM parallel tracking.

| Feature | UI Component | New APIs |
|---|---|---|
| Create Opportunity | `<CreateOpportunityDrawer>` (LoB, Asset Category, Asset Class) | `POST /api/v1/opportunities` |
| Quote generation | `<QuoteGenerator>` rack rate display + deviation inputs | `GET /api/v1/quotes/rack-rate`, `POST /api/v1/quotes` |
| Quote approval workflow | `<QuoteApprovalBanner>` + notification badge | `POST /api/v1/quotes/{id}/approve` |
| Quote versions list | `<QuoteVersionHistory>` | `GET /api/v1/quotes/{id}/versions` |
| Application initiation | `<ApplicationForm>` pre-filled from Prospect | `POST /api/v1/applications` |
| KYC document checklist | `<KYCChecklist>` stage-gated mandatory docs | `GET /api/v1/applications/{id}/kyc-checklist` |
| CAM status panel | Real-time `<CAMStatusCard>` | `GET /api/v1/applications/{id}/cam-status` |
| Parallel KYC + CAM progress | Two-track progress indicator | WebSocket or polling `/api/v1/applications/{id}/status` |
| Fraud screening result | `<FraudScreeningResult>` Hunter/Sherlock/Clear | `GET /api/v1/applications/{id}/fraud-status` |

---

### Phase 5 (Weeks 37–42) — Customer Creation & Full Analytics

**Deliverable:** Customer gate display, ECID issuance, full lineage view, all 8 role-based dashboards.

| Feature | UI Component | New APIs |
|---|---|---|
| Customer creation gate | `<CustomerGatePanel>` KYC=Complete + CAM=Approved indicator | `POST /api/v1/customers` |
| Enterprise Customer ID display | `<ECIDCard>` similar to EnquiryNumberCard | `GET /api/v1/customers/{ecid}` |
| Full lineage chain | `<FullLineageChain>` Lead → Prospect → Opp → Quote → App → Customer | `GET /api/v1/lineage/{id}` |
| Role assignment | `<RoleAssignmentModule>` Lessee/Dealer/Depositor | `POST /api/v1/customers/{ecid}/roles` |
| All 8 dashboards | My To-Do, Lead Pipeline, Prospect Pipeline, Exception Queue, SLA Compliance, Aging, Performance, Marketing Source | Multiple analytics APIs |
| KPI report export | CSV download for compliance | `GET /api/v1/reports/{type}?format=csv` |

---

## 10. Architecture Gap Register

| # | Gap | Severity | Description | Recommended Resolution |
|---|---|---|---|---|
| G1 | **Quarkus vs Spring Boot** | **High** | EA mandates Spring Boot + Clean Architecture. Existing `lead-service` uses Quarkus (Hibernate Panache + RESTEasy Reactive). The API contract (OpenAPI) is correct, but the internal framework diverges from the EA mandate. | Raise an ADR: either (a) approve Quarkus as an EA-approved alternative (comparable container footprint, same Clean Architecture principles apply), or (b) migrate to Spring Boot before Phase 3. UI is unaffected either way. |
| G2 | **No Dapr sidecar on lead-service** | High | The existing `lead-service` has no Dapr annotations or pub/sub integration. Kafka events (LeadCaptured) are not being published. | Add Dapr annotations to `k8s/lead-service-deployment.yaml`; implement `DaprClient` pub/sub in the service layer before Phase 2. |
| G3 | **No Keycloak realm provisioned** | High | The prototype uses mock header-based auth (`X-User-Id`). Kong JWT plugin requires a Keycloak realm and client configuration. | Provision `sf-leasing` realm in Keycloak; configure `leasing-ui` client with PKCE; configure `leasing-svc` client for service accounts. This is a Phase 1 Week 1 prerequisite. |
| G4 | **No Kong configuration** | High | No Kong Ingress Controller routes are defined for any service. | Create `KongIngress` CRDs for all Phase 1 services; configure JWT plugin, rate-limiting plugin, and request-transformer (to inject `X-User-Id` from token `sub`). |
| G5 | **Reference data service missing** | Medium | The Contract Form and Applicant Form require reference APIs (asset taxonomy, dealers, pincodes). No such service exists yet. | Build a lightweight `reference-data-service` (Spring Boot or Quarkus) with static + configurable data; expose via Kong in Phase 1. |
| G6 | **DMS adapter missing** | Medium | KYC document upload to DMS is referenced but no adapter service exists. | Build `dms-adapter` microservice in Phase 1 (Week 6–7); supports Test/Beta/Live via `ENV_INDICATOR` system parameter. |
| G7 | **SQL injection in LeadResource** | **Critical Security** | `LeadResource.java` lines 95–98 build SQL queries via string concatenation from query parameters. This is a SQL injection vulnerability. | Replace with parameterised Panache queries: `Lead.find("status = ?1 AND ...", status, ...)`. Fix before any production deployment. |
| G8 | **No frontend app scaffolded** | Low | Only prototype JSX files exist; no React application, package.json, or build configuration is set up. | Create `ui/` directory at project root; scaffold with `npm create vite@latest` using React+TypeScript template; copy prototyped design system. |
| G9 | **Mobile IMEI unavailable in browser** | Low | `navigator.mediaDevices` cannot access device IMEI in a browser context. The prototype uses `Math.random()` as a stand-in. | For PWA: implement a native Capacitor plugin to retrieve IMEI on Android; for desktop browser, keep manual entry + `crypto.randomUUID()` for UUID; document this design decision in an ADR. |
| G10 | **No offline/PWA capability** | Low | Field Officers may operate in areas with poor connectivity. The enquiry wizard should support draft-save and offline creation. | Add service worker (Vite PWA plugin); queue failed `POST /api/v1/leads` requests via background sync. Phase 2 enhancement. |

---

## 11. Testing Strategy

### 11.1 Unit / Component Tests (Vitest + RTL)

- All design-system components: render, error state, disabled state
- Validation functions: PAN, Aadhaar, mobile, email, pincode
- `useEnquiryForm` hook: state transitions, localStorage persistence, reset
- LTV calculation: edge cases (0 cost, financeAmount > assetCost)
- Step navigation guards: cannot proceed with invalid form

### 11.2 Contract Tests (Vitest + MSW)

Mock Service Worker intercepts API calls in tests; contract tests verify the request shape the UI sends matches the OpenAPI spec the backend expects:

```typescript
// createLead.contract.test.ts
it('sends correct ApplicantRequest shape for NRI applicant', async () => {
  const requestBody = await captureRequest('POST', '/api/v1/leads');
  expect(requestBody.applicants[0]).toMatchSchema(ApplicantRequestSchema);
  expect(requestBody.applicants[0].passport).toBeDefined();
  expect(requestBody.applicants[0].passportExpiry).toBeDefined();
});
```

### 11.3 E2E Tests (Playwright)

Run against `staging-k8s-cluster` with real Keycloak and a seeded test branch:

| Scenario | Steps |
|---|---|
| Happy path — individual enquiry | Login → Contract → Applicant (PAN valid) → KYC upload → Review → Submit → Success with LRN |
| NRI applicant | Login → Applicant with isNRI=true → Passport + expiry validation → Submit |
| Pincode outside radius (mobile) | Login via mobile UA → Submit → LN3955 warning visible on Success screen |
| Exception queue routing | Submit without PAN+GSTIN+Mobile → Assert 202 Accepted → Exception queue entry visible in CPU dashboard |
| Aadhaar invalid | Enter 11-digit Aadhaar → Expect inline error → Cannot proceed to next step |
| Finance > Asset cost | Enter financeAmount > assetCost → Expect inline error |
| Role-based access | Field Officer cannot see Exception Queue (CPU-only route) |

### 11.4 Performance Testing (k6)

Target: 2,000 concurrent users submitting enquiries (NFR-S1):
```javascript
// k6 script
export const options = { vus: 2000, duration: '5m' };
export default function() {
  const res = http.post(`${BASE_URL}/api/v1/leads`, JSON.stringify(payload), { headers });
  check(res, { 'status 201': r => r.status === 201, 'p99 < 200ms': r => r.timings.duration < 200 });
}
```

---

## Summary: What Must Be Built for UI Phase 1

| Item | Type | Owner | Priority |
|---|---|---|---|
| `sf-leasing-ui` React+Vite project | New | Frontend team | P0 |
| Keycloak `sf-leasing` realm + `leasing-ui` client | Config | Platform/Infra | P0 |
| Kong Ingress routes for Phase 1 services | Config | Platform/Infra | P0 |
| Design system TypeScript components | New | Frontend team | P0 |
| Enquiry Wizard (7 steps) integrated with live APIs | New | Frontend team | P0 |
| `reference-data-service` (asset taxonomy, pincode, dealers) | New | Backend team | P0 |
| `dms-adapter` (KYC document upload) | New | Backend team | P1 |
| Fix SQL injection in `LeadResource.java` | Bug fix | Backend team | **P0 (security)** |
| Dapr sidecar + `LeadCaptured` Kafka event on lead creation | Enhancement | Backend team | P1 |
| Lead list view + exception queue view | New | Frontend team | P1 |
| Mobile responsive QA + Playwright E2E suite | Testing | QA | P1 |
