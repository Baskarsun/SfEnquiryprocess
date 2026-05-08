// Screens: Login, Contract Details, Applicant Details

// ─── Validation helpers ─────────────────────────────────────────
const validatePAN    = v => /^[A-Z]{5}[0-9]{4}[A-Z]$/.test(v.toUpperCase());
const validateAadhaar= v => /^\d{12}$/.test(v.replace(/\s/g,''));
const validateMobile = v => /^\d{10}$/.test(v);
const validateEmail  = v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
const validatePincode= v => /^\d{6}$/.test(v);

// ─── LOGIN SCREEN ───────────────────────────────────────────────
function LoginScreen({ onNext, formData, setFormData }) {
  const [loading, setLoading] = React.useState(false);
  const [authError, setAuthError] = React.useState('');
  const [touched, setTouched] = React.useState(false);

  const d = formData.user;
  const upd = (k, v) => setFormData(p => ({ ...p, user: { ...p.user, [k]: v } }));

  const errs = {
    userId: !d.userId.trim() ? 'Employee ID is required' : '',
    imei: !d.imei.trim() ? 'Device IMEI is required' : '',
  };
  const isValid = !Object.values(errs).some(Boolean);

  const autoDetect = () => {
    upd('imei', '35' + Math.random().toString().slice(2, 16));
    upd('uuid', crypto.randomUUID ? crypto.randomUUID() : 'uuid-' + Date.now());
    upd('appVersion', '4.2.1');
  };

  const handleLogin = async () => {
    setTouched(true);
    if (!isValid) return;
    setLoading(true); setAuthError('');
    await new Promise(r => setTimeout(r, 1800));
    setLoading(false);
    if (d.userId.toUpperCase() === 'BADUSER') {
      setAuthError('Authentication failed: User not found in system registry. Contact your branch administrator.');
    } else {
      onNext();
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: `linear-gradient(135deg, ${SF.navyDark} 0%, ${SF.navy} 50%, ${SF.navyLight} 100%)`, padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 440 }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 36 }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 16 }}>
            <SFLogo white />
          </div>
          <h1 style={{ color: '#FFF', fontSize: 22, fontWeight: 800, margin: 0 }}>Loan Enquiry Portal</h1>
          <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: 13, margin: '6px 0 0' }}>Field Officer Access — Authenticated Session</p>
        </div>

        {/* Card */}
        <div style={{ background: 'rgba(255,255,255,0.97)', borderRadius: 18, padding: 32, boxShadow: '0 20px 60px rgba(0,0,0,0.3)' }}>
          <div style={{ fontSize: 16, fontWeight: 700, color: SF.navy, marginBottom: 22 }}>Sign In</div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <SFInput
              label="Employee ID" required placeholder="e.g. EMP001"
              value={d.userId} onChange={v => { upd('userId', v); setAuthError(''); }}
              error={touched && errs.userId}
              helper="Your 6-character employee code"
            />

            <div>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
                <div style={{ flex: 1 }}>
                  <SFInput
                    label="Device IMEI" required placeholder="15-digit IMEI number"
                    value={d.imei} onChange={v => upd('imei', v)}
                    error={touched && errs.imei}
                  />
                </div>
                <button onClick={autoDetect} style={{ padding: '10px 14px', background: SF.goldBg, border: `1px solid ${SF.gold}`, borderRadius: 8, color: SF.gold, fontSize: 12, fontWeight: 700, cursor: 'pointer', whiteSpace: 'nowrap', fontFamily: 'inherit' }}>
                  Auto-detect
                </button>
              </div>
              {d.uuid && <div style={{ marginTop: 8 }}>
                <SFInput label="Device UUID" value={d.uuid} onChange={() => {}} readOnly />
              </div>}
              {d.appVersion && <div style={{ marginTop: 8 }}>
                <SFInput label="App Version" value={d.appVersion} onChange={() => {}} readOnly suffix="current" />
              </div>}
            </div>

            {authError && <SFAlert type="error">{authError}</SFAlert>}

            <div style={{ padding: '12px 14px', background: SF.goldBg, borderRadius: 8, border: `1px solid ${SF.goldLight}` }}>
              <div style={{ fontSize: 12, color: SF.warning, fontWeight: 600, marginBottom: 4 }}>⚠ Security Notice</div>
              <div style={{ fontSize: 12, color: SF.muted, lineHeight: 1.5 }}>This portal is for authorised Sundaram Finance employees only. All sessions are logged and monitored.</div>
            </div>

            <SFButton onClick={handleLogin} fullWidth size="lg" loading={loading}>
              {loading ? 'Authenticating...' : 'Authenticate & Continue'}
            </SFButton>
          </div>

          <div style={{ textAlign: 'center', marginTop: 18, fontSize: 12, color: SF.muted }}>
            Having trouble? Contact IT Helpdesk · <span style={{ color: SF.navy, fontWeight: 600 }}>1800-XXX-XXXX</span>
          </div>
        </div>

        <div style={{ textAlign: 'center', marginTop: 20, color: 'rgba(255,255,255,0.25)', fontSize: 11 }}>
          © 2025 Sundaram Finance Limited · Powered by TCS
        </div>
      </div>
    </div>
  );
}

// ─── CONTRACT DETAILS FORM ──────────────────────────────────────
function ContractForm({ onNext, onBack, formData, setFormData }) {
  const [touched, setTouched] = React.useState(false);
  const d = formData.contract;
  const upd = (k, v) => setFormData(p => ({ ...p, contract: { ...p.contract, [k]: v } }));

  const errs = {
    enquiryDate: !d.enquiryDate ? 'Enquiry date is required' : '',
    contractType: !d.contractType ? 'Contract type is required' : '',
    assetMake: !d.assetMake.trim() ? 'Asset make is required' : '',
    assetModel: !d.assetModel.trim() ? 'Asset model is required' : '',
    assetCost: !d.assetCost ? 'Asset cost is required' : Number(d.assetCost) <= 0 ? 'Must be greater than zero' : '',
    financeAmount: !d.financeAmount ? 'Finance amount is required' : Number(d.financeAmount) > Number(d.assetCost) ? 'Cannot exceed asset cost' : '',
    loanTenure: !d.loanTenure ? 'Tenure is required' : '',
    dealerCode: !d.dealerCode.trim() ? 'Dealer code is required' : '',
  };
  const isValid = !Object.values(errs).some(Boolean);

  const handleNext = () => {
    setTouched(true);
    if (isValid) { onNext(); return; }
    // Scroll to top so user sees the first error
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const fErr = k => touched ? errs[k] : '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: '4px 0 24px' }}>
      <div>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: SF.navy }}>Contract Details</h2>
        <p style={{ margin: '4px 0 0', color: SF.muted, fontSize: 14 }}>Asset, loan, and business information for this enquiry</p>
      </div>

      {/* Enquiry info */}
      <SFCard title="Enquiry Information">
        <FormGrid>
          <SFInput label="Enquiry Date" required type="date" value={d.enquiryDate} onChange={v => upd('enquiryDate', v)} error={fErr('enquiryDate')} />
          <SFSelect label="Contract Type" required value={d.contractType} onChange={v => upd('contractType', v)} error={fErr('contractType')}
            placeholder="Select contract type"
            options={['Hire Purchase','Loan Against Asset','Lease','Hypothecation']} />
          <SFSelect label="Asset Class" value={d.assetClass} onChange={v => upd('assetClass', v)}
            placeholder="Select asset class"
            options={['Two Wheeler','Three Wheeler','Four Wheeler','Commercial Vehicle','Construction Equipment','Tractor']} />
          <SFSelect label="Loan Purpose" value={d.loanPurpose} onChange={v => upd('loanPurpose', v)}
            placeholder="Select purpose"
            options={['Personal Use','Business Use','Agricultural Use','Commercial Use']} />
        </FormGrid>
      </SFCard>

      {/* Asset details */}
      <SFCard title="Asset Details">
        <FormGrid>
          <SFInput label="Asset Make" required placeholder="e.g. Maruti, Honda, TATA" value={d.assetMake} onChange={v => upd('assetMake', v)} error={fErr('assetMake')} />
          <SFInput label="Asset Model" required placeholder="e.g. Swift VXi, Activa 6G" value={d.assetModel} onChange={v => upd('assetModel', v)} error={fErr('assetModel')} />
          <SFSelect label="Asset Type" value={d.assetType} onChange={v => upd('assetType', v)}
            placeholder="Select asset type"
            options={['New','Used / Pre-owned']} />
          <SFSelect label="Fuel Type" value={d.fuelType} onChange={v => upd('fuelType', v)}
            placeholder="Select fuel type"
            options={['Petrol','Diesel','Electric','CNG','Hybrid']} />
          <SFInput label="Asset Usage" placeholder="e.g. Private, Commercial" value={d.assetUsage} onChange={v => upd('assetUsage', v)} />
          <SFInput label="Number of Units" type="number" placeholder="1" value={d.numUnits} onChange={v => upd('numUnits', v)} />
        </FormGrid>
      </SFCard>

      {/* Financial details */}
      <SFCard title="Financial Details">
        <FormGrid>
          <SFInput label="Asset Cost (₹)" required type="number" placeholder="0.00" value={d.assetCost} onChange={v => upd('assetCost', v)} error={fErr('assetCost')} prefix="₹" />
          <SFInput label="Finance Amount (₹)" required type="number" placeholder="0.00" value={d.financeAmount} onChange={v => upd('financeAmount', v)} error={fErr('financeAmount')} prefix="₹" />
          <SFInput label="Loan Tenure" required type="number" placeholder="e.g. 36" value={d.loanTenure} onChange={v => upd('loanTenure', v)} error={fErr('loanTenure')} suffix="months" />
          <SFInput label="Quoted Interest Rate" type="number" placeholder="e.g. 12.50" value={d.interestRate} onChange={v => upd('interestRate', v)} suffix="% p.a." />
          <SFInput label="Net Income (₹/month)" type="number" placeholder="0.00" value={d.netIncome} onChange={v => upd('netIncome', v)} prefix="₹" />
          <SFSelect label="Repayment Frequency" value={d.repaymentFreq} onChange={v => upd('repaymentFreq', v)}
            placeholder="Select frequency"
            options={['Monthly','Quarterly','Half-Yearly','Annually']} />
        </FormGrid>

        {/* LTV indicator */}
        {d.assetCost && d.financeAmount && Number(d.assetCost) > 0 && (
          <div style={{ marginTop: 14, padding: '10px 14px', background: SF.infoBg, borderRadius: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 13, color: SF.info }}>Loan-to-Value (LTV)</span>
            <span style={{ fontSize: 16, fontWeight: 800, color: SF.navy }}>
              {Math.min(100, Math.round((Number(d.financeAmount) / Number(d.assetCost)) * 100))}%
            </span>
          </div>
        )}
      </SFCard>

      {/* Business & dealer */}
      <SFCard title="Business & Dealer Information">
        <FormGrid>
          <SFInput label="Dealer Code" required placeholder="e.g. DLR00123" value={d.dealerCode} onChange={v => upd('dealerCode', v)} error={fErr('dealerCode')} />
          <SFSelect label="Business Source" value={d.businessSource} onChange={v => upd('businessSource', v)}
            placeholder="Select source"
            options={['Field Officer','DSA / Agent','Branch Walk-in','Online Lead','OEM Referral','Existing Customer']} />
          <SFSelect label="Appraisal Category" value={d.appraisalCategory} onChange={v => upd('appraisalCategory', v)}
            placeholder="Select category"
            options={['Standard','Managed','GECL','Priority']} />
          <SFSelect label="Assessment Criteria" value={d.assessmentCriteria} onChange={v => upd('assessmentCriteria', v)}
            placeholder="Select criteria"
            options={['Income-based','Asset-based','Combined']} />
          <SFInput label="Marketing Employee ID" placeholder="Employee ID" value={d.marketingEmployee} onChange={v => upd('marketingEmployee', v)} />
          <SFInput label="Lead Management ID" placeholder="LMS Reference (optional)" value={d.lmsLeadId} onChange={v => upd('lmsLeadId', v)} helper="From external lead management system" />
        </FormGrid>
        <div style={{ marginTop: 16 }}>
          <SFInput label="Remarks" placeholder="Any additional notes..." value={d.remarks} onChange={v => upd('remarks', v)} />
        </div>
      </SFCard>

      <NavButtons onBack={onBack} onNext={handleNext} backLabel="← Back to Login" nextLabel="Save & Continue →" />
    </div>
  );
}

// ─── APPLICANT FORM ─────────────────────────────────────────────
const EMPTY_APPLICANT = () => ({
  type: 'MAIN APPLICANT', name: '', dob: '', gender: '', constitution: 'INDIVIDUAL',
  pan: '', aadhaar: '', dl: '', passport: '', passportExpiry: '', voterId: '',
  mobile: '', altMobile: '', email: '', smsConsent: true,
  addrLine1: '', addrLine2: '', pincode: '', city: '', state: '', isNRI: false,
});

function ApplicantForm({ onNext, onBack, formData, setFormData }) {
  const [activeIdx, setActiveIdx] = React.useState(0);
  const [touched, setTouched] = React.useState({});

  const applicants = formData.applicants;
  const setApplicants = (fn) => setFormData(p => ({ ...p, applicants: typeof fn === 'function' ? fn(p.applicants) : fn }));
  const upd = (idx, k, v) => setApplicants(prev => prev.map((a, i) => i === idx ? { ...a, [k]: v } : a));

  const addApplicant = () => {
    if (applicants.length >= 4) return;
    setApplicants(prev => [...prev, { ...EMPTY_APPLICANT(), type: `CO-APPLICANT ${prev.length}` }]);
    setActiveIdx(applicants.length);
  };
  const removeApplicant = (idx) => {
    if (idx === 0) return;
    setApplicants(prev => prev.filter((_, i) => i !== idx));
    setActiveIdx(Math.max(0, idx - 1));
  };

  const a = applicants[activeIdx] || EMPTY_APPLICANT();
  const t = touched[activeIdx] || false;
  const touchCurrent = () => setTouched(prev => ({ ...prev, [activeIdx]: true }));

  const errs = {
    name: !a.name.trim() ? 'Full name is required' : '',
    pan: a.pan && !validatePAN(a.pan) ? 'Invalid PAN format (e.g. ABCDE1234F)' : '',
    aadhaar: a.aadhaar && !validateAadhaar(a.aadhaar) ? 'Aadhaar must be exactly 12 digits' : '',
    mobile: !a.mobile ? 'Mobile number is required' : !validateMobile(a.mobile) ? 'Must be 10 digits' : '',
    altMobile: a.altMobile && !validateMobile(a.altMobile) ? 'Must be 10 digits' : '',
    email: a.email && !validateEmail(a.email) ? 'Invalid email address' : '',
    pincode: a.pincode && !validatePincode(a.pincode) ? 'Must be 6 digits' : '',
    dob: !a.dob ? 'Date of birth is required' : '',
    gender: a.constitution === 'INDIVIDUAL' && !a.gender ? 'Gender is required' : '',
    passportExpiry: a.isNRI && !a.passportExpiry ? 'Passport expiry required for NRI' : '',
    passport: a.isNRI && !a.passport.trim() ? 'Passport number required for NRI' : '',
  };

  const tabValid = (idx) => {
    const ap = applicants[idx];
    return ap.name.trim() && ap.mobile && validateMobile(ap.mobile) && ap.dob;
  };

  const handleNext = () => {
    touchCurrent();
    const currentErrs = Object.values(errs).some(Boolean);
    if (currentErrs) { window.scrollTo({ top: 0, behavior: 'smooth' }); return; }
    const allOk = applicants.every(ap => ap.name.trim() && ap.mobile && validateMobile(ap.mobile) && ap.dob);
    if (!allOk) {
      setTouched(applicants.reduce((acc, _, i) => ({ ...acc, [i]: true }), {}));
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    onNext();
  };

  const typeLabels = { 'MAIN APPLICANT': 'Main', 'CO-APPLICANT 1': 'Co-App 1', 'CO-APPLICANT 2': 'Co-App 2', 'CO-APPLICANT 3': 'Co-App 3' };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: '4px 0 24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: SF.navy }}>Applicant Details</h2>
          <p style={{ margin: '4px 0 0', color: SF.muted, fontSize: 14 }}>Add main applicant and up to 3 co-applicants</p>
        </div>
        {applicants.length < 4 && (
          <SFButton onClick={addApplicant} variant="outline" size="sm">+ Add Co-Applicant</SFButton>
        )}
      </div>

      {/* Tab bar */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {applicants.map((ap, i) => {
          const valid = tabValid(i);
          const isActive = i === activeIdx;
          return (
            <button key={i} onClick={() => setActiveIdx(i)} style={{
              display: 'flex', alignItems: 'center', gap: 7,
              padding: '8px 16px', borderRadius: 8, border: 'none',
              background: isActive ? SF.navy : SF.white, cursor: 'pointer', fontFamily: 'inherit',
              boxShadow: isActive ? '0 2px 10px rgba(27,58,107,0.25)' : '0 1px 4px rgba(0,0,0,0.08)',
              transition: 'all 0.15s',
            }}>
              <div style={{ width: 22, height: 22, borderRadius: '50%', background: isActive ? SF.gold : valid ? SF.success : SF.border, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#FFF', fontWeight: 700 }}>
                {valid ? '✓' : i + 1}
              </div>
              <span style={{ fontSize: 13, fontWeight: 600, color: isActive ? '#FFF' : SF.text }}>
                {typeLabels[ap.type] || `App ${i + 1}`}
              </span>
              {i > 0 && (
                <span onClick={e => { e.stopPropagation(); removeApplicant(i); }} style={{ marginLeft: 4, color: isActive ? 'rgba(255,255,255,0.5)' : SF.muted, fontSize: 14, lineHeight: 1, cursor: 'pointer' }}>×</span>
              )}
            </button>
          );
        })}
      </div>

      {/* Applicant type badge */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <SFBadge color={activeIdx === 0 ? SF.navy : SF.gold}>{a.type}</SFBadge>
        {activeIdx === 0 && <SFBadge color={SF.success}>Primary applicant</SFBadge>}
      </div>

      {/* Personal information */}
      <SFCard title="Personal Information">
        <FormGrid>
          <div style={{ gridColumn: '1 / -1' }}>
            <SFInput label="Full Name" required placeholder="As per identity document"
              value={a.name} onChange={v => upd(activeIdx, 'name', v)}
              error={t && errs.name} />
          </div>
          <SFInput label="Date of Birth" required type="date" value={a.dob} onChange={v => upd(activeIdx, 'dob', v)} error={t && errs.dob} />
          <SFSelect label="Gender" required={a.constitution === 'INDIVIDUAL'} value={a.gender} onChange={v => upd(activeIdx, 'gender', v)} error={t && errs.gender}
            placeholder="Select gender" options={['Male','Female','Third Gender']} />
          <SFSelect label="Constitution Type" value={a.constitution} onChange={v => upd(activeIdx, 'constitution', v)}
            options={['INDIVIDUAL','NON INDIVIDUAL']} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingTop: 22 }}>
            <input type="checkbox" id={`nri-${activeIdx}`} checked={a.isNRI} onChange={e => upd(activeIdx, 'isNRI', e.target.checked)} style={{ width: 16, height: 16, accentColor: SF.navy }} />
            <label htmlFor={`nri-${activeIdx}`} style={{ fontSize: 14, color: SF.text, cursor: 'pointer' }}>NRI Applicant</label>
          </div>
        </FormGrid>
        {a.isNRI && (
          <div style={{ marginTop: 16, padding: '14px', background: SF.infoBg, borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ fontSize: 12, fontWeight: 700, color: SF.info }}>NRI REQUIREMENTS</div>
            <FormGrid>
              <SFInput label="Passport Number" required placeholder="A1234567" value={a.passport} onChange={v => upd(activeIdx, 'passport', v)} error={t && errs.passport} />
              <SFInput label="Passport Expiry Date" required type="date" value={a.passportExpiry} onChange={v => upd(activeIdx, 'passportExpiry', v)} error={t && errs.passportExpiry} helper="Must be valid for 90+ more days" />
            </FormGrid>
          </div>
        )}
      </SFCard>

      {/* KYC Documents */}
      <SFCard title="KYC Identifiers">
        <FormGrid>
          <SFInput label="PAN Number" placeholder="ABCDE1234F" value={a.pan} onChange={v => upd(activeIdx, 'pan', v.toUpperCase())} error={t && errs.pan} maxLength={10} helper="Mandatory for income validation" />
          <SFInput label="Aadhaar Number" placeholder="1234 5678 9012" value={a.aadhaar} onChange={v => upd(activeIdx, 'aadhaar', v.replace(/\D/g,''))} error={t && errs.aadhaar} maxLength={12} />
          <SFInput label="Driving Licence" placeholder="TN-0120110012345" value={a.dl} onChange={v => upd(activeIdx, 'dl', v)} />
          <SFInput label="Voter ID" placeholder="ABC1234567" value={a.voterId} onChange={v => upd(activeIdx, 'voterId', v)} />
        </FormGrid>
        {a.pan && validatePAN(a.pan) && (
          <div style={{ marginTop: 10 }}>
            <SFAlert type="success">PAN format validated ✓</SFAlert>
          </div>
        )}
      </SFCard>

      {/* Contact */}
      <SFCard title="Contact Information">
        <FormGrid>
          <SFInput label="Mobile Number" required placeholder="10-digit mobile" value={a.mobile} onChange={v => upd(activeIdx, 'mobile', v.replace(/\D/g,''))} error={t && errs.mobile} maxLength={10} prefix="+91" />
          <SFInput label="Alternate Mobile" placeholder="10-digit mobile" value={a.altMobile} onChange={v => upd(activeIdx, 'altMobile', v.replace(/\D/g,''))} error={t && errs.altMobile} maxLength={10} prefix="+91" helper={a.isNRI ? 'Required for NRI' : ''} />
          <div style={{ gridColumn: '1 / -1' }}>
            <SFInput label="Email Address" placeholder="name@example.com" type="email" value={a.email} onChange={v => upd(activeIdx, 'email', v)} error={t && errs.email} helper={a.isNRI ? 'Required for NRI applicants' : ''} />
          </div>
        </FormGrid>
        <div style={{ marginTop: 14, display: 'flex', alignItems: 'center', gap: 10 }}>
          <input type="checkbox" id={`sms-${activeIdx}`} checked={a.smsConsent} onChange={e => upd(activeIdx, 'smsConsent', e.target.checked)} style={{ width: 16, height: 16, accentColor: SF.navy }} />
          <label htmlFor={`sms-${activeIdx}`} style={{ fontSize: 13, color: SF.text, cursor: 'pointer' }}>I consent to receive SMS notifications regarding this loan enquiry</label>
        </div>
      </SFCard>

      {/* Address */}
      <SFCard title="Residential Address">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <SFInput label="Address Line 1" placeholder="House / Flat No., Street" value={a.addrLine1} onChange={v => upd(activeIdx, 'addrLine1', v)} />
          <SFInput label="Address Line 2" placeholder="Area, Locality" value={a.addrLine2} onChange={v => upd(activeIdx, 'addrLine2', v)} />
          <FormGrid>
            <SFInput label="Pincode" placeholder="6-digit pincode" value={a.pincode} onChange={v => upd(activeIdx, 'pincode', v.replace(/\D/g,''))} error={t && errs.pincode} maxLength={6} />
            <SFInput label="City" placeholder="City name" value={a.city} onChange={v => upd(activeIdx, 'city', v)} />
            <SFSelect label="State" value={a.state} onChange={v => upd(activeIdx, 'state', v)} placeholder="Select state"
              options={['Andhra Pradesh','Assam','Bihar','Delhi','Goa','Gujarat','Haryana','Karnataka','Kerala','Madhya Pradesh','Maharashtra','Odisha','Punjab','Rajasthan','Tamil Nadu','Telangana','Uttar Pradesh','West Bengal']} />
            <SFInput label="Location Stability (years)" type="number" placeholder="Years at this address" value={a.locationStability} onChange={v => upd(activeIdx, 'locationStability', v)} />
          </FormGrid>
        </div>
      </SFCard>

      <NavButtons onBack={onBack} onNext={handleNext} nextLabel="Continue to KYC →" />
    </div>
  );
}

Object.assign(window, { LoginScreen, ContractForm, ApplicantForm, validatePAN, validateAadhaar, validateMobile, validateEmail, validatePincode, EMPTY_APPLICANT });
