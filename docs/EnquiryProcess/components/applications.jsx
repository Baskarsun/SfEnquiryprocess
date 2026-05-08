
// ─── Application Origination + Customer Creation ─────────────────────────────

function ApplicationsPage({ role, setPage }) {
  const app = window.SF_APPLICATIONS[0];

  const streamStatus = [
    { label: 'AML Screening', code: 'AML', status: app.aml, color: app.aml==='Clear'?'#69DB7C':'#FFB81C', icon: '🛡', detail: 'AML1 complete · No sanctions match · Re-KYC not due' },
    { label: 'KYC Validation', code: 'KYC', status: app.kyc, color: app.kyc==='Complete'?'#69DB7C':'#FFB81C', icon: '📋', detail: 'All documents uploaded and verified' },
    { label: 'CKYC Registry', code: 'CKYC', status: app.ckyc, color: app.ckyc==='Verified'?'#69DB7C':'#FFB81C', icon: '🏛', detail: 'Central KYC Registry — verified' },
    { label: 'Credit Appraisal (CAM)', code: 'CAM', status: app.cam, color: app.cam==='Approved'?'#69DB7C':app.cam==='In Review'?'#FFB81C':'#ADB5BD', icon: '📊', detail: `Assigned to: ${app.camApprover}` },
  ];

  return (
    <div>
      <div style={acStyles.pageHeader}>
        <div>
          <div style={acStyles.stageTag}>⑤ STAGE 5</div>
          <div style={acStyles.pageTitle}>Application Origination</div>
          <div style={acStyles.pageSub}>Parallel KYC · AML · CKYC · Credit Appraisal (CAM)</div>
        </div>
        <button style={acStyles.primaryBtn}>+ Initiate Application</button>
      </div>

      {/* Application Header Card */}
      <div style={acStyles.appHeaderCard}>
        <div style={acStyles.appHeaderLeft}>
          <div style={acStyles.appIdBig}>{app.id}</div>
          <div style={acStyles.appName}>{app.applicantName}</div>
          <div style={acStyles.appMeta}>
            <span>Prospect: {app.prospectId}</span>
            <span>·</span>
            <span>Quote: {app.quoteId}</span>
            <span>·</span>
            <span>{app.assetClass}</span>
            <span>·</span>
            <span>{app.leaseType}</span>
          </div>
        </div>
        <div style={acStyles.appHeaderRight}>
          <div style={acStyles.appAmount}>₹{(app.assetCost/1000000).toFixed(0)}M</div>
          <div style={acStyles.appAmountLabel}>Asset Cost</div>
          <div style={{marginTop:8}}><StatusChip status={app.status} /></div>
        </div>
      </div>

      {/* Parallel Streams */}
      <div style={acStyles.streamsGrid}>
        {streamStatus.map((s, i) => (
          <div key={i} style={{...acStyles.streamCard, borderTop:`3px solid ${s.color}`}}>
            <div style={acStyles.streamTop}>
              <span style={acStyles.streamIcon}>{s.icon}</span>
              <div style={{...acStyles.streamCode, color: s.color}}>{s.code}</div>
              <StatusChip status={s.status} />
            </div>
            <div style={acStyles.streamLabel}>{s.label}</div>
            <div style={acStyles.streamDetail}>{s.detail}</div>
            {(s.code==='KYC'||s.code==='AML') && s.status!=='Complete' && s.status!=='Clear' && (
              <button style={acStyles.streamAction}>View Details →</button>
            )}
            {s.status==='Complete'||s.status==='Clear'||s.status==='Verified' ? (
              <div style={acStyles.streamComplete}>✓ Gate Passed</div>
            ) : s.code==='CAM' ? (
              <div style={{...acStyles.streamComplete, color:'#FFB81C'}}>⏳ Awaiting CAM decision</div>
            ) : null}
          </div>
        ))}
      </div>

      {/* AML Re-KYC Gate + BOD Gate */}
      <div style={acStyles.gatesRow}>
        <div style={acStyles.gateCard}>
          <div style={acStyles.gateTitle}>AML6 — Re-KYC Gate</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C'}}>✓ Not Blocked</div>
          <div style={acStyles.gateNote}>Re-KYC not pending for any Lessee / Co-Lessee / Dealer / DMA</div>
        </div>
        <div style={acStyles.gateCard}>
          <div style={acStyles.gateTitle}>AML8 — BOD Gate</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C'}}>✓ Not Blocked</div>
          <div style={acStyles.gateNote}>Beneficial Ownership Declaration complete for all linked parties</div>
        </div>
      </div>

      {/* Document Checklist */}
      <div style={acStyles.card}>
        <div style={acStyles.cardHeader}>
          <span style={acStyles.cardTitle}>Document Checklist</span>
          <span style={{fontSize:12, color:'#8896A9'}}>{app.docs.filter(d=>d.status==='Uploaded').length}/{app.docs.length} uploaded</span>
        </div>
        <div style={acStyles.docsGrid}>
          {app.docs.map((d, i) => (
            <div key={i} style={{...acStyles.docRow, background: d.status==='Uploaded'?'#F0FFF4':'#FFF9E6'}}>
              <span style={{color: d.status==='Uploaded'?'#69DB7C':'#FFB81C', fontSize:16}}>
                {d.status==='Uploaded'?'✓':'⏳'}
              </span>
              <span style={acStyles.docName}>{d.name}</span>
              <StatusChip status={d.status} />
            </div>
          ))}
        </div>
      </div>

      {/* CAM Notes */}
      <div style={acStyles.card}>
        <div style={acStyles.cardHeader}><span style={acStyles.cardTitle}>Credit Appraisal Notes</span></div>
        <div style={acStyles.camNotes}>
          <div style={acStyles.camApprover}>Assigned to: <strong>{app.camApprover}</strong> · Status: <StatusChip status={app.cam} /></div>
          <div style={acStyles.camNote}>"{app.camNotes}"</div>
        </div>
        <button style={{...acStyles.primaryBtn, marginTop:12, fontSize:12}} onClick={() => setPage('customers')}>
          Promote to Customer (when KYC=Complete & CAM=Approved) →
        </button>
      </div>
    </div>
  );
}

// ─── Customer Creation & Lineage ─────────────────────────────────────────────

function CustomersPage({ role }) {
  const cust = window.SF_CUSTOMERS[0];

  return (
    <div>
      <div style={acStyles.pageHeader}>
        <div>
          <div style={acStyles.stageTag6}>⑥ STAGE 6</div>
          <div style={acStyles.pageTitle}>Enterprise Customer Creation</div>
          <div style={acStyles.pageSub}>ECID assignment · Lineage tracking · Role tagging · AML risk profiling</div>
        </div>
      </div>

      {/* ECID Card */}
      <div style={acStyles.ecidCard}>
        <div style={acStyles.ecidLeft}>
          <div style={acStyles.ecidLabel}>ENTERPRISE CUSTOMER ID</div>
          <div style={acStyles.ecidNum}>{cust.ecid}</div>
          <div style={acStyles.ecidName}>{cust.name}</div>
        </div>
        <div style={acStyles.ecidRight}>
          <div style={acStyles.ecidMeta}><span style={acStyles.ecidKey}>Created</span><span style={acStyles.ecidVal}>{cust.created}</span></div>
          <div style={acStyles.ecidMeta}><span style={acStyles.ecidKey}>KYC Gate</span><span style={{...acStyles.ecidVal, color:'#69DB7C'}}>✓ {cust.kyc}</span></div>
          <div style={acStyles.ecidMeta}><span style={acStyles.ecidKey}>CAM Gate</span><span style={{...acStyles.ecidVal, color:'#69DB7C'}}>✓ {cust.cam}</span></div>
          <div style={acStyles.ecidMeta}><span style={acStyles.ecidKey}>AML Risk</span><span style={{...acStyles.ecidVal, color:'#69DB7C'}}>{cust.amlRisk}</span></div>
          <div style={acStyles.ecidMeta}><span style={acStyles.ecidKey}>Roles</span><span style={acStyles.ecidVal}>{cust.roles.join(', ')}</span></div>
        </div>
      </div>

      {/* Full Lineage */}
      <div style={acStyles.card}>
        <div style={acStyles.cardHeader}><span style={acStyles.cardTitle}>Full Origination Lineage</span></div>
        <div style={acStyles.lineage}>
          {[
            { id: cust.leadRef, label: 'Lead', stage: 1, color: '#4DABF7' },
            { id: cust.prospectId, label: 'Prospect', stage: 2, color: '#69DB7C' },
            { id: 'OPP-2026-00032', label: 'Opportunity', stage: 3, color: '#FFA94D' },
            { id: 'Q-202600013', label: 'Quote (Locked)', stage: 4, color: '#DA77F2' },
            { id: cust.applicationId, label: 'Application', stage: 5, color: '#F783AC' },
            { id: cust.ecid, label: 'Customer (ECID)', stage: 6, color: '#63E6BE', highlight: true },
          ].map((l, i) => (
            <React.Fragment key={i}>
              <div style={{...acStyles.lineageNode, ...(l.highlight ? {background:'#002060'} : {})}}>
                <div style={{...acStyles.lineageStage, color: l.color}}>{l.stage}</div>
                <div style={{...acStyles.lineageLabel, color: l.highlight?'#FFB81C':'#8896A9'}}>{l.label}</div>
                <div style={{...acStyles.lineageId, color: l.highlight?'#fff':'#002060'}}>{l.id}</div>
              </div>
              {i < 5 && <div style={acStyles.lineageArrow}>→</div>}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* Customer Creation Gates */}
      <div style={acStyles.gatesRow}>
        <div style={{...acStyles.gateCard, background:'#F0FFF4', border:'1px solid #69DB7C'}}>
          <div style={acStyles.gateTitle}>KYC Gate</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C', fontSize:22}}>✓ Complete</div>
          <div style={acStyles.gateNote}>All KYC documents verified and stored in DMS</div>
        </div>
        <div style={{...acStyles.gateCard, background:'#F0FFF4', border:'1px solid #69DB7C'}}>
          <div style={acStyles.gateTitle}>CAM Gate</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C', fontSize:22}}>✓ Approved</div>
          <div style={acStyles.gateNote}>Credit Appraisal sanctioned by Narayanan R</div>
        </div>
        <div style={{...acStyles.gateCard, background:'#F0FFF4', border:'1px solid #69DB7C'}}>
          <div style={acStyles.gateTitle}>AML Risk Profile</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C', fontSize:22}}>Low Risk</div>
          <div style={acStyles.gateNote}>Risk categorised per AML4 matrix · Next review: 31-Dec-2026</div>
        </div>
        <div style={{...acStyles.gateCard, background:'#F0FFF4', border:'1px solid #69DB7C'}}>
          <div style={acStyles.gateTitle}>BOD Gate</div>
          <div style={{...acStyles.gateStatus, color:'#69DB7C', fontSize:22}}>✓ Complete</div>
          <div style={acStyles.gateNote}>Beneficial Ownership Declaration received and recorded</div>
        </div>
      </div>

      {/* Role Assignment */}
      <div style={acStyles.card}>
        <div style={acStyles.cardHeader}><span style={acStyles.cardTitle}>Customer Role Assignment</span></div>
        <div style={acStyles.rolesGrid}>
          {['Lessee','Co-Lessee','Guarantor','Dealer','DMA/DSA','Depositor'].map((r,i)=>(
            <div key={i} style={{...acStyles.roleChip, ...(cust.roles.includes(r)?acStyles.roleChipActive:{})}}>
              <span style={{fontSize:14}}>{cust.roles.includes(r)?'✓':'○'}</span> {r}
            </div>
          ))}
        </div>
      </div>

      {/* Next Step */}
      <div style={acStyles.nextStep}>
        <div style={acStyles.nextStepIcon}>🎉</div>
        <div>
          <div style={acStyles.nextStepTitle}>Customer {cust.ecid} created successfully</div>
          <div style={acStyles.nextStepSub}>Proceed to Contract Configuration → (out of scope for this document)</div>
        </div>
        <button style={{...acStyles.primaryBtn, marginLeft:'auto'}}>Go to Contract Configuration →</button>
      </div>
    </div>
  );
}

const acStyles = {
  pageHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20 },
  stageTag: { fontSize:10, fontWeight:800, color:'#F783AC', letterSpacing:2, marginBottom:4 },
  stageTag6: { fontSize:10, fontWeight:800, color:'#63E6BE', letterSpacing:2, marginBottom:4 },
  pageTitle: { fontSize:24, fontWeight:900, color:'#002060', letterSpacing:-0.5 },
  pageSub: { fontSize:12, color:'#8896A9', marginTop:2 },
  primaryBtn: { padding:'10px 22px', background:'#002060', color:'#FFB81C', border:'none', borderRadius:10, fontWeight:800, fontSize:13, cursor:'pointer' },
  appHeaderCard: { background:'#002060', borderRadius:14, padding:'20px 24px', display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20, boxShadow:'0 4px 16px rgba(0,32,96,0.2)' },
  appHeaderLeft: {},
  appIdBig: { fontFamily:'monospace', fontSize:13, color:'rgba(255,255,255,0.5)', marginBottom:4 },
  appName: { fontSize:22, fontWeight:900, color:'#fff', marginBottom:6 },
  appMeta: { display:'flex', gap:8, fontSize:11, color:'rgba(255,255,255,0.5)' },
  appHeaderRight: { textAlign:'right' },
  appAmount: { fontSize:36, fontWeight:900, color:'#FFB81C' },
  appAmountLabel: { fontSize:11, color:'rgba(255,255,255,0.4)', marginTop:2 },
  streamsGrid: { display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:16, marginBottom:20 },
  streamCard: { background:'#fff', borderRadius:12, padding:18, boxShadow:'0 1px 4px rgba(0,32,96,0.07)' },
  streamTop: { display:'flex', alignItems:'center', gap:8, marginBottom:10, justifyContent:'space-between' },
  streamIcon: { fontSize:20 },
  streamCode: { fontWeight:900, fontSize:14 },
  streamLabel: { fontSize:12, fontWeight:700, color:'#002060', marginBottom:6 },
  streamDetail: { fontSize:11, color:'#8896A9', marginBottom:10 },
  streamAction: { fontSize:11, color:'#0050B3', background:'#EEF4FF', border:'none', borderRadius:6, padding:'4px 10px', cursor:'pointer', fontWeight:700 },
  streamComplete: { fontSize:11, color:'#69DB7C', fontWeight:700 },
  gatesRow: { display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:12, marginBottom:20 },
  gateCard: { background:'#fff', borderRadius:12, padding:16, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', border:'1px solid #E0E4EA' },
  gateTitle: { fontSize:11, fontWeight:700, color:'#8896A9', marginBottom:6 },
  gateStatus: { fontSize:16, fontWeight:800, marginBottom:4 },
  gateNote: { fontSize:11, color:'#8896A9' },
  card: { background:'#fff', borderRadius:14, padding:20, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', marginBottom:16 },
  cardHeader: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:14 },
  cardTitle: { fontSize:14, fontWeight:800, color:'#002060' },
  docsGrid: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 },
  docRow: { display:'flex', alignItems:'center', gap:10, padding:'8px 12px', borderRadius:8 },
  docName: { flex:1, fontSize:12, color:'#2D3748' },
  camNotes: { padding:14, background:'#F8FAFC', borderRadius:8 },
  camApprover: { fontSize:12, color:'#5A6A7E', marginBottom:8 },
  camNote: { fontSize:13, color:'#2D3748', fontStyle:'italic' },
  ecidCard: { background:'linear-gradient(135deg, #002060 0%, #0050B3 100%)', borderRadius:16, padding:24, display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:20, boxShadow:'0 8px 24px rgba(0,32,96,0.25)' },
  ecidLeft: {},
  ecidLabel: { fontSize:10, color:'rgba(255,255,255,0.4)', letterSpacing:2, fontWeight:700, marginBottom:6 },
  ecidNum: { fontSize:28, fontWeight:900, color:'#FFB81C', fontFamily:'monospace' },
  ecidName: { fontSize:14, color:'rgba(255,255,255,0.7)', marginTop:4 },
  ecidRight: { display:'flex', flexDirection:'column', gap:6 },
  ecidMeta: { display:'flex', gap:16, justifyContent:'space-between', alignItems:'center' },
  ecidKey: { fontSize:11, color:'rgba(255,255,255,0.4)' },
  ecidVal: { fontSize:12, fontWeight:700, color:'#fff' },
  lineage: { display:'flex', alignItems:'center', gap:0, overflowX:'auto', padding:'8px 0' },
  lineageNode: { background:'#F8FAFC', border:'1px solid #E0E4EA', borderRadius:10, padding:'10px 14px', textAlign:'center', flexShrink:0, minWidth:110 },
  lineageStage: { fontSize:18, fontWeight:900, marginBottom:3 },
  lineageLabel: { fontSize:10, fontWeight:600, marginBottom:4 },
  lineageId: { fontSize:10, fontFamily:'monospace', fontWeight:700 },
  lineageArrow: { fontSize:20, color:'#C4CDD8', margin:'0 6px', flexShrink:0 },
  rolesGrid: { display:'flex', gap:10, flexWrap:'wrap' },
  roleChip: { padding:'8px 16px', borderRadius:20, border:'1px solid #E0E4EA', background:'#F8FAFC', fontSize:12, color:'#8896A9', display:'flex', alignItems:'center', gap:6 },
  roleChipActive: { background:'#EEF4FF', color:'#002060', border:'1px solid #4DABF7', fontWeight:700 },
  nextStep: { display:'flex', alignItems:'center', gap:16, padding:20, background:'#F0FFF4', borderRadius:14, border:'1px solid #69DB7C' },
  nextStepIcon: { fontSize:28 },
  nextStepTitle: { fontSize:14, fontWeight:800, color:'#1A6B1A' },
  nextStepSub: { fontSize:12, color:'#5A6A7E', marginTop:2 },
};

Object.assign(window, { ApplicationsPage, CustomersPage, acStyles });
