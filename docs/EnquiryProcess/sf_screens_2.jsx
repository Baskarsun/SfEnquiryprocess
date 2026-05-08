// Screens: KYC Upload, Review, Processing, Success/Error

// ─── KYC DOCUMENT UPLOAD ────────────────────────────────────────
function KYCUpload({ onNext, onBack, formData, setFormData }) {
  const [dragging, setDragging] = React.useState(false);
  const fileInputRef = React.useRef();

  const files = formData.kyc.files;
  const setFiles = (fn) => setFormData(p => ({ ...p, kyc: { ...p.kyc, files: typeof fn === 'function' ? fn(p.kyc.files) : fn } }));

  const getDocType = (name) => {
    const n = name.toLowerCase();
    if (n.includes('photo') || n.includes('pic') || n.includes('selfie')) return { label: 'Photo', color: SF.gold, icon: '🤳' };
    if (n.includes('pan')) return { label: 'PAN Card', color: SF.navy, icon: '🪪' };
    if (n.includes('aadhaar') || n.includes('aadhar')) return { label: 'Aadhaar', color: '#7C3AED', icon: '🆔' };
    if (n.includes('licence') || n.includes('license') || n.includes('dl')) return { label: 'Driving Licence', color: SF.success, icon: '🚗' };
    if (n.includes('passport')) return { label: 'Passport', color: '#0891B2', icon: '🛂' };
    if (n.includes('voter')) return { label: 'Voter ID', color: '#BE185D', icon: '🗳️' };
    return { label: 'KYC Document', color: SF.muted, icon: '📄' };
  };

  const formatSize = (bytes) => bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;

  const addFiles = (fileList) => {
    const newFiles = Array.from(fileList).map(f => ({
      id: Date.now() + Math.random(),
      name: f.name, size: f.size, file: f,
      docType: getDocType(f.name),
      applicantType: 'MAIN APPLICANT',
    }));
    setFiles(prev => [...prev, ...newFiles]);
  };

  const handleDrop = (e) => { e.preventDefault(); setDragging(false); addFiles(e.dataTransfer.files); };
  const removeFile = (id) => setFiles(prev => prev.filter(f => f.id !== id));

  const docGroups = [
    { key: 'photo', label: 'Applicant Photo', icon: '🤳', required: false, hint: 'Recent passport-size photograph' },
    { key: 'pan', label: 'PAN Card', icon: '🪪', required: false, hint: 'Self-attested copy' },
    { key: 'aadhaar', label: 'Aadhaar Card', icon: '🆔', required: false, hint: 'Both sides if applicable' },
    { key: 'address', label: 'Address Proof', icon: '🏠', required: false, hint: 'Utility bill, bank statement, etc.' },
    { key: 'income', label: 'Income Proof', icon: '💼', required: false, hint: 'Salary slip, ITR, or bank statement' },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: '4px 0 24px' }}>
      <div>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: SF.navy }}>KYC Documents</h2>
        <p style={{ margin: '4px 0 0', color: SF.muted, fontSize: 14 }}>Upload identity and supporting documents for all applicants</p>
      </div>

      {/* Drop zone */}
      <div
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current.click()}
        style={{
          border: `2px dashed ${dragging ? SF.gold : SF.border}`,
          borderRadius: 14, padding: '36px 24px', textAlign: 'center', cursor: 'pointer',
          background: dragging ? SF.goldBg : '#FAFCFF', transition: 'all 0.2s',
        }}
      >
        <div style={{ fontSize: 40, marginBottom: 10 }}>📁</div>
        <div style={{ fontSize: 15, fontWeight: 700, color: SF.navy, marginBottom: 6 }}>
          {dragging ? 'Drop files here' : 'Drag & drop files, or click to browse'}
        </div>
        <div style={{ fontSize: 13, color: SF.muted }}>Supports JPG, PNG, PDF · Max 10 MB per file</div>
        <div style={{ marginTop: 14 }}>
          <span style={{ display: 'inline-block', padding: '8px 20px', background: SF.navy, color: '#FFF', borderRadius: 8, fontSize: 13, fontWeight: 600 }}>Choose Files</span>
        </div>
        <input ref={fileInputRef} type="file" multiple accept=".jpg,.jpeg,.png,.pdf" style={{ display: 'none' }} onChange={e => addFiles(e.target.files)} />
      </div>

      {/* Document checklist */}
      <SFCard title="Document Checklist">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {docGroups.map(dg => {
            const uploaded = files.filter(f => f.docType.label.toLowerCase().includes(dg.key) || (dg.key === 'photo' && f.docType.icon === '🤳')).length > 0;
            return (
              <div key={dg.key} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px', borderRadius: 8, background: uploaded ? SF.successBg : '#F8FAFF', border: `1px solid ${uploaded ? SF.success + '40' : SF.border}` }}>
                <span style={{ fontSize: 20 }}>{dg.icon}</span>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: SF.text }}>{dg.label}</div>
                  <div style={{ fontSize: 12, color: SF.muted }}>{dg.hint}</div>
                </div>
                {dg.required && <SFBadge color={SF.error}>Required</SFBadge>}
                <span style={{ fontSize: 18 }}>{uploaded ? '✅' : '⬜'}</span>
              </div>
            );
          })}
        </div>
      </SFCard>

      {/* Uploaded files */}
      {files.length > 0 && (
        <SFCard title={`Uploaded Files (${files.length})`}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {files.map(f => (
              <div key={f.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px', borderRadius: 8, background: '#F8FAFF', border: `1px solid ${SF.border}` }}>
                <span style={{ fontSize: 22 }}>{f.docType.icon}</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: SF.text, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{f.name}</div>
                  <div style={{ display: 'flex', gap: 8, marginTop: 3, flexWrap: 'wrap' }}>
                    <SFBadge color={f.docType.color}>{f.docType.label}</SFBadge>
                    <span style={{ fontSize: 11, color: SF.muted }}>{formatSize(f.size)}</span>
                  </div>
                </div>
                <select value={f.applicantType} onChange={e => setFiles(prev => prev.map(x => x.id === f.id ? { ...x, applicantType: e.target.value } : x))}
                  style={{ fontSize: 12, border: `1px solid ${SF.border}`, borderRadius: 6, padding: '4px 8px', fontFamily: 'inherit', color: SF.text, background: '#FFF' }}>
                  {formData.applicants.map((a, i) => <option key={i} value={a.type}>{a.type}</option>)}
                </select>
                <button onClick={() => removeFile(f.id)} style={{ border: 'none', background: 'none', cursor: 'pointer', color: SF.error, fontSize: 18, padding: 4 }}>×</button>
              </div>
            ))}
          </div>
        </SFCard>
      )}

      <SFAlert type="info">
        All documents will be encrypted and uploaded to the DMS after enquiry creation. Ensure photos are clear and legible.
      </SFAlert>

      <NavButtons onBack={onBack} onNext={onNext} nextLabel="Review & Submit →" />
    </div>
  );
}

// ─── REVIEW SCREEN ───────────────────────────────────────────────
function ReviewScreen({ onNext, onBack, formData, onEditStep }) {
  const [expanded, setExpanded] = React.useState({ contract: true, applicants: false, kyc: false });
  const toggle = k => setExpanded(p => ({ ...p, [k]: !p[k] }));

  const c = formData.contract;
  const pairs = (items) => (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 20px' }}>
      {items.filter(([,v]) => v).map(([label, value]) => (
        <div key={label} style={{ padding: '8px 0', borderBottom: `1px solid ${SF.border}` }}>
          <div style={{ fontSize: 11, color: SF.muted, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em' }}>{label}</div>
          <div style={{ fontSize: 13, color: SF.text, fontWeight: 500, marginTop: 2 }}>{value}</div>
        </div>
      ))}
    </div>
  );

  const Section = ({ title, sectionKey, editStep, children }) => (
    <div style={{ background: SF.white, borderRadius: 12, overflow: 'hidden', boxShadow: '0 2px 12px rgba(27,58,107,0.07)' }}>
      <button onClick={() => toggle(sectionKey)} style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 20px', background: '#FAFCFF', border: 'none', cursor: 'pointer', fontFamily: 'inherit', borderBottom: `1px solid ${SF.border}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 3, height: 16, background: SF.gold, borderRadius: 2 }} />
          <span style={{ fontSize: 14, fontWeight: 700, color: SF.navy }}>{title}</span>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <span onClick={e => { e.stopPropagation(); onEditStep(editStep); }} style={{ fontSize: 12, color: SF.navy, fontWeight: 600, textDecoration: 'underline', cursor: 'pointer' }}>Edit</span>
          <span style={{ color: SF.muted, fontSize: 14 }}>{expanded[sectionKey] ? '▲' : '▼'}</span>
        </div>
      </button>
      {expanded[sectionKey] && <div style={{ padding: 20 }}>{children}</div>}
    </div>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, padding: '4px 0 24px' }}>
      <div>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 800, color: SF.navy }}>Review & Confirm</h2>
        <p style={{ margin: '4px 0 0', color: SF.muted, fontSize: 14 }}>Verify all details before submitting the enquiry</p>
      </div>

      {/* Summary strip */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 12 }}>
        {[
          { label: 'Contract Type', value: c.contractType || '—' },
          { label: 'Asset Cost', value: c.assetCost ? `₹${Number(c.assetCost).toLocaleString('en-IN')}` : '—' },
          { label: 'Applicants', value: formData.applicants.length },
        ].map(item => (
          <div key={item.label} style={{ background: SF.white, borderRadius: 10, padding: '14px 16px', boxShadow: '0 2px 8px rgba(27,58,107,0.08)', textAlign: 'center' }}>
            <div style={{ fontSize: 20, fontWeight: 800, color: SF.navy }}>{item.value}</div>
            <div style={{ fontSize: 11, color: SF.muted, marginTop: 3 }}>{item.label}</div>
          </div>
        ))}
      </div>

      {/* Contract section */}
      <Section title="Contract Details" sectionKey="contract" editStep={1}>
        {pairs([
          ['Enquiry Date', c.enquiryDate],
          ['Contract Type', c.contractType],
          ['Asset Make', c.assetMake],
          ['Asset Model', c.assetModel],
          ['Asset Type', c.assetType],
          ['Fuel Type', c.fuelType],
          ['Asset Cost', c.assetCost ? `₹${Number(c.assetCost).toLocaleString('en-IN')}` : ''],
          ['Finance Amount', c.financeAmount ? `₹${Number(c.financeAmount).toLocaleString('en-IN')}` : ''],
          ['Loan Tenure', c.loanTenure ? `${c.loanTenure} months` : ''],
          ['Interest Rate', c.interestRate ? `${c.interestRate}% p.a.` : ''],
          ['Dealer Code', c.dealerCode],
          ['Business Source', c.businessSource],
          ['Appraisal Category', c.appraisalCategory],
          ['LMS Lead ID', c.lmsLeadId],
        ])}
      </Section>

      {/* Applicants section */}
      <Section title={`Applicants (${formData.applicants.length})`} sectionKey="applicants" editStep={2}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {formData.applicants.map((a, i) => (
            <div key={i}>
              {i > 0 && <div style={{ height: 1, background: SF.border, margin: '0 0 16px' }} />}
              <div style={{ marginBottom: 10 }}>
                <SFBadge color={i === 0 ? SF.navy : SF.gold}>{a.type}</SFBadge>
              </div>
              {pairs([
                ['Full Name', a.name],
                ['Date of Birth', a.dob],
                ['Gender', a.gender],
                ['Constitution', a.constitution],
                ['PAN', a.pan],
                ['Aadhaar', a.aadhaar ? '••••••••' + a.aadhaar.slice(-4) : ''],
                ['Mobile', a.mobile ? `+91 ${a.mobile}` : ''],
                ['Email', a.email],
                ['Pincode', a.pincode],
                ['State', a.state],
                ['NRI', a.isNRI ? 'Yes' : ''],
                ['Passport', a.passport],
              ])}
            </div>
          ))}
        </div>
      </Section>

      {/* KYC section */}
      <Section title={`KYC Documents (${formData.kyc.files.length} uploaded)`} sectionKey="kyc" editStep={3}>
        {formData.kyc.files.length === 0
          ? <div style={{ color: SF.muted, fontSize: 13 }}>No documents uploaded. You may proceed without documents.</div>
          : <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {formData.kyc.files.map((f, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', background: '#F8FAFF', borderRadius: 8 }}>
                  <span>{f.docType.icon}</span>
                  <span style={{ fontSize: 13, flex: 1 }}>{f.name}</span>
                  <SFBadge color={f.docType.color}>{f.docType.label}</SFBadge>
                </div>
              ))}
            </div>
        }
      </Section>

      <SFAlert type="warning">
        By submitting, you confirm all information is accurate. The system will authenticate applicants against caution lists, perform KYC de-duplication, and initiate fraud screening. Misrepresentation is a legal offence.
      </SFAlert>

      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, paddingTop: 4 }}>
        <SFButton onClick={onBack} variant="ghost">← Back</SFButton>
        <SFButton onClick={onNext} variant="gold" size="lg">
          Submit Enquiry ▶
        </SFButton>
      </div>
    </div>
  );
}

// ─── PROCESSING SCREEN ──────────────────────────────────────────
function ProcessingScreen({ onDone, formData }) {
  const [stepIndex, setStepIndex] = React.useState(0);
  const [result, setResult] = React.useState(null);
  const [pincodeWarning, setPincodeWarning] = React.useState(false);
  const [priceUpdate, setPriceUpdate] = React.useState(false);

  const steps = [
    { label: 'Authenticating user & device', duration: 900, icon: '🔐' },
    { label: 'Validating input completeness', duration: 600, icon: '📋' },
    { label: 'Resolving branch & employee record', duration: 700, icon: '🏢' },
    { label: 'KYC de-duplication & caution check', duration: 1400, icon: '🔍' },
    { label: 'Geographic / pincode validation', duration: 1100, icon: '📍' },
    { label: 'Product model market value retrieval', duration: 900, icon: '💰' },
    { label: 'Appraisal category validation', duration: 600, icon: '✅' },
    { label: 'Creating enquiry record', duration: 1200, icon: '💾' },
    { label: 'Initiating fraud screening (Hunter/Sherlock)', duration: 1500, icon: '🛡️' },
    { label: 'Requesting CIBIL credit bureau report', duration: 1000, icon: '📊' },
    { label: 'Uploading KYC documents to DMS', duration: formData.kyc.files.length > 0 ? 1300 : 400, icon: '📤' },
    { label: 'Finalising & generating enquiry number', duration: 800, icon: '🎉' },
  ];

  React.useEffect(() => {
    let cancelled = false;
    const run = async () => {
      for (let i = 0; i < steps.length; i++) {
        if (cancelled) return;
        setStepIndex(i);
        await new Promise(r => setTimeout(r, steps[i].duration));
        if (i === 4) setPincodeWarning(true);
        if (i === 5) setPriceUpdate(true);
      }
      if (!cancelled) {
        const enquiryNum = `SFL/${new Date().getFullYear().toString().slice(-2)}/${String(Math.floor(Math.random() * 9000) + 1000).padStart(4,'0')}/${String(Math.floor(Math.random() * 90000) + 10000)}`;
        setResult({ success: true, enquiryNumber: enquiryNum });
        onDone({ success: true, enquiryNumber: enquiryNum, pincodeWarning, priceUpdate });
      }
    };
    run();
    return () => { cancelled = true; };
  }, []);

  const total = steps.length;
  const pct = Math.round(((stepIndex + 1) / total) * 100);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '32px 0', gap: 28, minHeight: 500, justifyContent: 'center' }}>
      {/* Animated progress ring */}
      <div style={{ position: 'relative', width: 120, height: 120 }}>
        <svg width="120" height="120" style={{ transform: 'rotate(-90deg)' }}>
          <circle cx="60" cy="60" r="52" fill="none" stroke={SF.border} strokeWidth="8" />
          <circle cx="60" cy="60" r="52" fill="none" stroke={SF.gold} strokeWidth="8"
            strokeDasharray={`${2 * Math.PI * 52}`}
            strokeDashoffset={`${2 * Math.PI * 52 * (1 - pct / 100)}`}
            strokeLinecap="round"
            style={{ transition: 'stroke-dashoffset 0.6s ease' }}
          />
        </svg>
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column' }}>
          <div style={{ fontSize: 24, fontWeight: 800, color: SF.navy }}>{pct}%</div>
          <div style={{ fontSize: 10, color: SF.muted }}>PROCESSING</div>
        </div>
      </div>

      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 18, fontWeight: 700, color: SF.navy }}>Processing Your Enquiry</div>
        <div style={{ fontSize: 13, color: SF.muted, marginTop: 4 }}>Please do not close this window</div>
      </div>

      {/* Steps list */}
      <div style={{ width: '100%', maxWidth: 480, display: 'flex', flexDirection: 'column', gap: 6 }}>
        {steps.map((s, i) => {
          const done = i < stepIndex;
          const active = i === stepIndex;
          const pending = i > stepIndex;
          return (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '9px 14px', borderRadius: 9,
              background: active ? SF.goldBg : done ? SF.successBg : 'transparent',
              border: `1px solid ${active ? SF.gold + '60' : done ? SF.success + '30' : 'transparent'}`,
              opacity: pending ? 0.35 : 1, transition: 'all 0.3s',
            }}>
              <div style={{
                width: 26, height: 26, borderRadius: '50%', flexShrink: 0,
                background: done ? SF.success : active ? SF.gold : SF.border,
                display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12,
              }}>
                {done ? <span style={{ color: '#FFF', fontWeight: 700, fontSize: 13 }}>✓</span>
                  : active ? <span style={{ display: 'inline-block', animation: 'spin 0.7s linear infinite', color: '#FFF', fontSize: 13 }}>⟳</span>
                  : <span style={{ fontSize: 13 }}>{s.icon}</span>}
              </div>
              <div style={{ flex: 1, fontSize: 13, fontWeight: active ? 600 : 400, color: active ? SF.navy : done ? SF.success : SF.muted }}>
                {s.label}
              </div>
              {done && <span style={{ fontSize: 13, color: SF.success }}>✓</span>}
            </div>
          );
        })}
      </div>

      {pincodeWarning && (
        <SFAlert type="warning">Geographic check: applicant pincode is outside standard branch service radius. Proceeding with override.</SFAlert>
      )}
      {priceUpdate && (
        <SFAlert type="info">Asset cost updated to current NDLP state-level dealer price.</SFAlert>
      )}
    </div>
  );
}

// ─── SUCCESS SCREEN ──────────────────────────────────────────────
function SuccessScreen({ result, formData, onNewEnquiry, onModify }) {
  const [revealed, setRevealed] = React.useState(false);
  const [copied, setCopied] = React.useState(false);

  React.useEffect(() => {
    setTimeout(() => setRevealed(true), 300);
  }, []);

  const copyNumber = () => {
    if (navigator.clipboard) navigator.clipboard.writeText(result.enquiryNumber);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const mainApplicant = formData.applicants[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '24px 0 40px', gap: 24 }}>
      {/* Celebration header */}
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 56, marginBottom: 12, animation: 'bounceIn 0.5s ease' }}>🎉</div>
        <h2 style={{ margin: 0, fontSize: 24, fontWeight: 800, color: SF.navy }}>Enquiry Created Successfully!</h2>
        <p style={{ color: SF.muted, marginTop: 6, fontSize: 14 }}>
          Loan enquiry for <strong>{mainApplicant?.name || 'Applicant'}</strong> has been registered
        </p>
      </div>

      {/* Enquiry number card */}
      <div style={{
        width: '100%', maxWidth: 420,
        background: `linear-gradient(135deg, ${SF.navyDark} 0%, ${SF.navy} 60%, ${SF.navyLight} 100%)`,
        borderRadius: 18, padding: '28px 28px 24px', color: '#FFF',
        boxShadow: '0 12px 40px rgba(27,58,107,0.35)',
        transform: revealed ? 'scale(1)' : 'scale(0.85)',
        opacity: revealed ? 1 : 0,
        transition: 'all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1)',
        position: 'relative', overflow: 'hidden',
      }}>
        {/* Decorative circles */}
        <div style={{ position: 'absolute', top: -30, right: -30, width: 120, height: 120, borderRadius: '50%', background: 'rgba(200,153,43,0.12)' }} />
        <div style={{ position: 'absolute', bottom: -20, left: -20, width: 80, height: 80, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }} />

        <div style={{ position: 'relative' }}>
          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', letterSpacing: '0.15em', marginBottom: 6 }}>ENQUIRY REFERENCE NUMBER</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: '0.04em', color: SF.gold, marginBottom: 16, wordBreak: 'break-all' }}>
            {result.enquiryNumber}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px 20px', marginBottom: 18 }}>
            {[
              ['Contract Type', formData.contract.contractType],
              ['Asset', `${formData.contract.assetMake} ${formData.contract.assetModel}`],
              ['Finance Amount', formData.contract.financeAmount ? `₹${Number(formData.contract.financeAmount).toLocaleString('en-IN')}` : '—'],
              ['Date', new Date().toLocaleDateString('en-IN')],
            ].map(([l,v]) => (
              <div key={l}>
                <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.4)', letterSpacing: '0.08em' }}>{l.toUpperCase()}</div>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'rgba(255,255,255,0.9)', marginTop: 2 }}>{v || '—'}</div>
              </div>
            ))}
          </div>

          <button onClick={copyNumber} style={{
            display: 'flex', alignItems: 'center', gap: 7, padding: '8px 18px', borderRadius: 8,
            background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)',
            color: '#FFF', fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: 'inherit',
            transition: 'all 0.15s',
          }}>
            {copied ? '✓ Copied!' : '📋 Copy Enquiry Number'}
          </button>
        </div>
      </div>

      {/* Status indicators */}
      <div style={{ width: '100%', maxWidth: 420, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {[
          { icon: '🛡️', label: 'Fraud Screening', status: 'Initiated', color: SF.warning },
          { icon: '📊', label: 'CIBIL Request', status: 'Submitted', color: SF.info },
          { icon: formData.kyc.files.length > 0 ? '📤' : '📄', label: 'KYC Documents', status: formData.kyc.files.length > 0 ? `${formData.kyc.files.length} uploaded` : 'Skipped', color: formData.kyc.files.length > 0 ? SF.success : SF.muted },
          { icon: '✅', label: 'Enquiry Status', status: 'Pending Review', color: SF.navy },
        ].map(item => (
          <div key={item.label} style={{ background: SF.white, borderRadius: 10, padding: '12px 14px', boxShadow: '0 2px 8px rgba(27,58,107,0.07)' }}>
            <div style={{ fontSize: 20 }}>{item.icon}</div>
            <div style={{ fontSize: 11, color: SF.muted, marginTop: 6 }}>{item.label}</div>
            <SFBadge color={item.color}>{item.status}</SFBadge>
          </div>
        ))}
      </div>

      {/* Advisories */}
      {(result.pincodeWarning || result.priceUpdate) && (
        <div style={{ width: '100%', maxWidth: 420, display: 'flex', flexDirection: 'column', gap: 8 }}>
          {result.pincodeWarning && <SFAlert type="warning">Pincode was outside standard branch service radius. Branch manager has been notified for review.</SFAlert>}
          {result.priceUpdate && <SFAlert type="info">Asset cost was updated to current NDLP state-level dealer price during processing.</SFAlert>}
        </div>
      )}

      {/* Next steps */}
      <div style={{ width: '100%', maxWidth: 420, background: SF.white, borderRadius: 12, padding: '18px 20px', boxShadow: '0 2px 10px rgba(27,58,107,0.07)' }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: SF.navy, marginBottom: 12 }}>Next Steps</div>
        {[
          { icon: '1', text: 'Share the enquiry number with the applicant for reference' },
          { icon: '2', text: 'Branch team will review fraud screening & CIBIL results' },
          { icon: '3', text: 'Applicant will be contacted for further documentation if needed' },
          { icon: '4', text: 'Field officer receives SMS / email notification on status change' },
        ].map(item => (
          <div key={item.icon} style={{ display: 'flex', gap: 10, marginBottom: 10, alignItems: 'flex-start' }}>
            <div style={{ width: 20, height: 20, borderRadius: '50%', background: SF.goldLight, color: SF.navy, fontSize: 11, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>{item.icon}</div>
            <div style={{ fontSize: 13, color: SF.text, lineHeight: 1.45 }}>{item.text}</div>
          </div>
        ))}
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', justifyContent: 'center' }}>
        <SFButton onClick={onModify} variant="outline">Modify This Enquiry</SFButton>
        <SFButton onClick={onNewEnquiry} variant="gold" size="lg">+ New Enquiry</SFButton>
      </div>
    </div>
  );
}

// ─── ERROR SCREEN ────────────────────────────────────────────────
function ErrorScreen({ error, onRetry, onBack }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '40px 0', gap: 20, textAlign: 'center' }}>
      <div style={{ fontSize: 56 }}>⚠️</div>
      <div>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 800, color: SF.error }}>Enquiry Submission Failed</h2>
        <p style={{ color: SF.muted, marginTop: 6, fontSize: 14 }}>The following error was returned by the system</p>
      </div>
      <div style={{ width: '100%', maxWidth: 460, background: SF.errorBg, border: `1px solid ${SF.error}40`, borderRadius: 12, padding: '18px 22px' }}>
        <div style={{ fontSize: 13, color: SF.error, fontWeight: 600, marginBottom: 6 }}>ERROR DETAILS</div>
        <div style={{ fontSize: 14, color: SF.text, lineHeight: 1.5 }}>{error || 'An unexpected error occurred. Please try again or contact support.'}</div>
      </div>
      <div style={{ display: 'flex', gap: 12 }}>
        <SFButton onClick={onBack} variant="ghost">← Go Back</SFButton>
        <SFButton onClick={onRetry} variant="primary">Retry Submission</SFButton>
      </div>
    </div>
  );
}

Object.assign(window, { KYCUpload, ReviewScreen, ProcessingScreen, SuccessScreen, ErrorScreen });
