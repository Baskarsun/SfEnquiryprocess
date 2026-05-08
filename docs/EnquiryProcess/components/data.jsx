
// ─── Mock Data & Constants ───────────────────────────────────────────────────

window.SF_ROLES = [
  { id: 'field_officer', label: 'Field Officer', icon: '👤' },
  { id: 'branch_manager', label: 'Branch Manager', icon: '🏢' },
  { id: 'cpu', label: 'CPU / Central Team', icon: '🖥️' },
  { id: 'pricing_analyst', label: 'Pricing Analyst', icon: '📊' },
  { id: 'aml_officer', label: 'AML Officer', icon: '🛡️' },
];

window.SF_LEADS = [
  { id: 'LS-202504-000101', name: 'Rajesh Kumar', type: 'Individual', mobile: '9876543210', pan: 'ABCPK1234A', status: 'Hot', stage: 'Qualified', source: 'Dealer', assignee: 'Arjun Mehta', dedup: 'New', created: '2026-04-20', asset: 'Commercial Vehicle' },
  { id: 'LS-202504-000102', name: 'Infosys Ltd', type: 'Commercial', mobile: '9988776655', gstin: '29AABCI1234C1Z5', status: 'Warm', stage: 'In Review', source: 'Campaign', assignee: 'Priya Sharma', dedup: 'PossibleExisting', created: '2026-04-21', asset: 'IT Equipment' },
  { id: 'LS-202504-000103', name: 'Meena Subramaniam', type: 'Individual', mobile: '9123456789', pan: 'BCDPM9876B', status: 'Cold', stage: 'New', source: 'Internal', assignee: 'Unassigned', dedup: 'Unknown', created: '2026-04-22', asset: 'Passenger Vehicle' },
  { id: 'LS-202504-000104', name: 'Tata Motors Finance', type: 'Commercial', mobile: '9000012345', gstin: '27AAACT2727Q1ZV', status: 'Hot', stage: 'Qualified', source: 'Direct', assignee: 'Kiran Nair', dedup: 'New', created: '2026-04-22', asset: 'Construction Equipment' },
  { id: 'LS-202504-000105', name: 'Anand Krishnamurthy', type: 'Individual', mobile: '9456781230', pan: 'CDEPA5678C', status: 'Warm', stage: 'In Review', source: 'Mobile App', assignee: 'Arjun Mehta', dedup: 'New', created: '2026-04-23', asset: 'Medical Equipment' },
  { id: 'LS-202504-000106', name: 'Microsoft India Pvt Ltd', type: 'Commercial', mobile: '8800990011', gstin: '07AAACM1234A1Z1', status: 'Hot', stage: 'Qualified', source: 'Campaign', assignee: 'Priya Sharma', dedup: 'PossibleExisting', created: '2026-04-23', asset: 'IT Equipment' },
];

window.SF_PROSPECTS = [
  {
    id: 'PR-2026-000045', leadRef: 'LS-202504-000101', name: 'Rajesh Kumar',
    type: 'Individual', pan: 'ABCPK1234A', panValid: true,
    status: 'Active', amlStatus: 'Clear', cibilStatus: 'Submitted',
    hunterResult: 'P', kycStatus: 'In Progress',
    branch: 'Chennai Main', assignee: 'Arjun Mehta',
    created: '2026-04-20', contractType: 'Finance Lease',
    asset: 'Commercial Vehicle', lob: 'Leasing',
    applicants: [
      { sl: 1, name: 'Rajesh Kumar', role: 'Lessee', pan: 'ABCPK1234A', aml: 'Clear', kyc: 'In Progress' },
    ]
  },
  {
    id: 'PR-2026-000046', leadRef: 'LS-202504-000104', name: 'Tata Motors Finance',
    type: 'Commercial', gstin: '27AAACT2727Q1ZV', panValid: true,
    status: 'Active', amlStatus: 'Flagged', cibilStatus: 'Submitted',
    hunterResult: 'M', kycStatus: 'In Progress',
    branch: 'Mumbai Central', assignee: 'Kiran Nair',
    created: '2026-04-22', contractType: 'Operating Lease',
    asset: 'Construction Equipment', lob: 'Leasing',
    applicants: [
      { sl: 1, name: 'Tata Motors Finance', role: 'Lessee', gstin: '27AAACT2727Q1ZV', aml: 'Flagged', kyc: 'Pending' },
      { sl: 2, name: 'Rajan Sharma', role: 'Guarantor', pan: 'ABCPS1234S', aml: 'Clear', kyc: 'Pending' },
    ]
  },
  {
    id: 'PR-2026-000047', leadRef: 'LS-202504-000106', name: 'Microsoft India Pvt Ltd',
    type: 'Commercial', gstin: '07AAACM1234A1Z1', panValid: true,
    status: 'Active', amlStatus: 'Clear', cibilStatus: 'Complete',
    hunterResult: 'P', kycStatus: 'Complete',
    branch: 'Delhi NCR', assignee: 'Priya Sharma',
    created: '2026-04-23', contractType: 'Operating Lease',
    asset: 'IT Equipment', lob: 'Leasing', preferredCustomer: true,
    applicants: [
      { sl: 1, name: 'Microsoft India Pvt Ltd', role: 'Lessee', gstin: '07AAACM1234A1Z1', aml: 'Clear', kyc: 'Complete' },
    ]
  },
];

window.SF_OPPORTUNITIES = [
  { id: 'OPP-2026-00031', prospectId: 'PR-2026-000045', prospectName: 'Rajesh Kumar', lob: 'Leasing', assetCategory: 'Vehicle', assetClass: 'Commercial Vehicle', volume: 1, value: 2500000, status: 'Quoted', quoteId: 'Q-202600012' },
  { id: 'OPP-2026-00032', prospectId: 'PR-2026-000047', prospectName: 'Microsoft India Pvt Ltd', lob: 'Leasing', assetCategory: 'IT Assets', assetClass: 'IT Equipment', volume: 500, value: 75000000, status: 'QuoteLocked', quoteId: 'Q-202600013', preferred: true },
  { id: 'OPP-2026-00033', prospectId: 'PR-2026-000046', prospectName: 'Tata Motors Finance', lob: 'Leasing', assetCategory: 'Construction', assetClass: 'Construction Equipment', volume: 12, value: 48000000, status: 'Open', quoteId: null },
];

window.SF_RACK_RATES = [
  { id: 'R000000201', assetClass: 'Commercial Vehicle', leaseType: 'Finance Lease', tenure: 36, cof: 8.5, gst: 18, depRate: 15, residualType: 'On Asset Cost', residualPct: 10, lmf: 1.5, deposit: 10, atdr: 10.5, corpTax: 25, status: 'Active', effectiveFrom: '2026-04-01', effectiveTo: '2026-12-31' },
  { id: 'R000000202', assetClass: 'IT Equipment', leaseType: 'Operating Lease', tenure: 24, cof: 7.75, gst: 18, depRate: 40, residualType: 'Flat', residualPct: 5, lmf: 2.0, deposit: 15, atdr: 9.5, corpTax: 25, status: 'Active', effectiveFrom: '2026-04-01', effectiveTo: '2026-12-31' },
  { id: 'R000000203', assetClass: 'Construction Equipment', leaseType: 'Finance Lease', tenure: 48, cof: 9.0, gst: 18, depRate: 15, residualType: 'On WDV', residualPct: 20, lmf: 1.75, deposit: 20, atdr: 11.0, corpTax: 25, status: 'Active', effectiveFrom: '2026-04-01', effectiveTo: '2026-12-31' },
  { id: 'R000000204', assetClass: 'Medical Equipment', leaseType: 'Finance Lease', tenure: 60, cof: 8.25, gst: 18, depRate: 15, residualType: 'Flat', residualPct: 1, lmf: 1.25, deposit: 10, atdr: 10.0, corpTax: 25, status: 'Draft', effectiveFrom: '2026-05-01', effectiveTo: '2026-12-31' },
];

window.SF_QUOTES = [
  {
    id: 'Q-202600012', rackRate: 'R000000201', prospectId: 'PR-2026-000045', prospectName: 'Rajesh Kumar',
    assetClass: 'Commercial Vehicle', leaseType: 'Finance Lease', assetCost: 2500000,
    tenure: 36, frequency: 'Monthly', paymentType: 'AD',
    rackRental: 82500, proposedRental: 80000, variance: -3.03,
    negotiationReason: 'Preferred dealer customer', approvalTier: 'Manager Approval',
    approvalStatus: 'Pending', status: 'Negotiation',
    validFrom: '2026-04-22', validTo: '2026-05-22', version: 1
  },
  {
    id: 'Q-202600013', rackRate: 'R000000202', prospectId: 'PR-2026-000047', prospectName: 'Microsoft India Pvt Ltd',
    assetClass: 'IT Equipment', leaseType: 'Operating Lease', assetCost: 75000000,
    tenure: 24, frequency: 'Monthly', paymentType: 'AD',
    rackRental: 3500000, proposedRental: 3325000, variance: -5.0,
    negotiationReason: 'Preferred company rate – Microsoft India MOU', approvalTier: 'Pricing Head',
    approvalStatus: 'Approved', status: 'Locked',
    validFrom: '2026-04-23', validTo: '2026-06-23', version: 2
  },
];

window.SF_APPLICATIONS = [
  {
    id: 'APL-2026-00021', prospectId: 'PR-2026-000047', quoteId: 'Q-202600013',
    applicantName: 'Microsoft India Pvt Ltd', assetClass: 'IT Equipment',
    assetCost: 75000000, tenure: 24, leaseType: 'Operating Lease',
    status: 'Underwriting',
    aml: 'Clear', kyc: 'Complete', ckyc: 'Verified', cam: 'In Review',
    camApprover: 'Narayanan R', camNotes: 'Strong financials; group exposure within limits.',
    docs: [
      { name: 'Certificate of Incorporation', status: 'Uploaded' },
      { name: 'GST Certificate', status: 'Uploaded' },
      { name: 'Balance Sheet FY24', status: 'Uploaded' },
      { name: 'Balance Sheet FY25', status: 'Pending' },
      { name: 'Board Resolution', status: 'Uploaded' },
    ]
  }
];

window.SF_CUSTOMERS = [
  {
    ecid: 'ECID-2026-00198', name: 'Microsoft India Pvt Ltd',
    applicationId: 'APL-2026-00021', prospectId: 'PR-2026-000047',
    leadRef: 'LS-202504-000106',
    roles: ['Lessee'],
    kyc: 'Complete', cam: 'Approved', amlRisk: 'Low',
    lineage: ['LS-202504-000106', 'PR-2026-000047', 'OPP-2026-00032', 'Q-202600013', 'APL-2026-00021', 'ECID-2026-00198'],
    created: '2026-04-24'
  }
];

window.SF_KPI = {
  totalLeads: 142, hotLeads: 38, prospects: 67, opportunities: 54,
  quotesLocked: 29, applications: 18, customers: 11,
  conversionRate: 7.7, avgTat: 4.2, amlFlagged: 3, pendingKyc: 12
};
