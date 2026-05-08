
// ─── Prospect / Enquiry Creation ─────────────────────────────────────────────

function ProspectsPage({ role, setPage }) {
  const [selected, setSelected] = React.useState('PR-2026-000045');
  const prospect = window.SF_PROSPECTS.find(p => p.id === selected);

  return (
    <div>
      <div style={pStyles.pageHeader}>
        <div>
          <div style={pStyles.stageTag}>② STAGE 2</div>
          <div style={pStyles.pageTitle}>Enquiry / Prospect</div>
          <div style={pStyles.pageSub}>Identity validation · AML screening · Compliance gates · {window.SF_PROSPECTS.length} active prospects</div>
        </div>
        <button style={pStyles.primaryBtn}>+ New Prospect</button>
      </div>

      <div style={pStyles.splitView}>
        {/* Prospect List */}
        <div style={pStyles.listPanel}>
          {window.SF_PROSPECTS.map(p => (
            <div key={p.id} style={{...pStyles.card, ...(selected===p.id?pStyles.cardActive:{})}}
              onClick={() => setSelected(p.id)}>
              <div style={pStyles.cardTop}>
                <div>
                  <div style={pStyles.prospectName}>{p.name}</div>
                  <div style={pStyles.prospectId}>{p.id}</div>
                </div>
                <StatusChip status={p.status} />
              </div>
              <div style={pStyles.cardMeta}>
                <AMLBadge status={p.amlStatus} />
                <span style={pStyles.metaTag}>{p.contractType}</span>
                {p.preferredCustomer && <span style={pStyles.preferredTag}>⭐ Preferred</span>}
              </div>
              <div style={pStyles.cardMeta}>
                <span style={pStyles.metaItem}>🏢 {p.branch}</span>
                <span style={pStyles.metaItem}>👤 {p.assignee}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Prospect Detail */}
        {prospect && <ProspectDetail prospect={prospect} setPage={setPage} />}
      </div>
    </div>
  );
}

function ProspectDetail({ prospect, setPage }) {
  const [activeTab, setActiveTab] = React.useState('overview');
  const tabs = ['overview','identity','aml','compliance','applicants'];

  return (
    <div style={pStyles.detailPanel}>
      {/* Detail Header */}
      <div style={pStyles.detailHeader}>
        <div>
          <div style={{display:'flex', alignItems:'center', gap:8}}>
            <div style={pStyles.detailName}>{prospect.name}</div>
            {prospect.preferredCustomer && <span style={pStyles.preferredBadge}>⭐ PREFERRED CUSTOMER</span>}
          </div>
          <div style={pStyles.detailMeta}>
            <span style={pStyles.idChip}>{prospect.id}</span>
            <span style={pStyles.metaItem}>← Lead: {prospect.leadRef}</span>
            <span style={pStyles.metaItem}>{prospect.created}</span>
          </div>
        </div>
        <div style={{display:'flex', gap:8, flexDirection:'column', alignItems:'flex-end'}}>
          <div style={{display:'flex', gap:8}}>
            <StatusChip status={prospect.status} />
            <AMLBadge status={prospect.amlStatus} />
          </div>
          <button style={pStyles.convertBtn} onClick={() => setPage('opportunities')}>
            Create Opportunity →
          </button>
        </div>
      </div>

      {/* Compliance Status Bar */}
      <div style={pStyles.complianceBar}>
        {[
          { label:'PAN / GSTIN', status: prospect.panValid ? 'Valid' : 'Failed', color: prospect.panValid?'#69DB7C':'#FF4D4D' },
          { label:'AML Screening', status: prospect.amlStatus, color: prospect.amlStatus==='Clear'?'#69DB7C':prospect.amlStatus==='Flagged'?'#FF4D4D':'#FFB81C' },
          { label:'CIBIL Bureau', status: prospect.cibilStatus, color: prospect.cibilStatus==='Complete'?'#69DB7C':'#FFB81C' },
          { label:'Hunter / Sherlock', status: prospect.hunterResult==='P'?'Pass':prospect.hunterResult==='F'?'Fail':'Manual', color: prospect.hunterResult==='P'?'#69DB7C':prospect.hunterResult==='F'?'#FF4D4D':'#FFB81C' },
          { label:'KYC', status: prospect.kycStatus, color: prospect.kycStatus==='Complete'?'#69DB7C':prospect.kycStatus==='In Progress'?'#FFB81C':'#FF4D4D' },
        ].map((c, i) => (
          <div key={i} style={pStyles.complianceItem}>
            <div style={{...pStyles.complianceDot, background: c.color}} />
            <div>
              <div style={pStyles.complianceLabel}>{c.label}</div>
              <div style={{...pStyles.complianceStatus, color: c.color}}>{c.status}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div style={pStyles.tabBar}>
        {tabs.map(t => (
          <button key={t} style={{...pStyles.tab, ...(activeTab===t?pStyles.tabActive:{})}}
            onClick={() => setActiveTab(t)}>{t.toUpperCase()}</button>
        ))}
      </div>

      <div style={pStyles.tabContent}>
        {activeTab === 'overview' && <ProspectOverview prospect={prospect} setPage={setPage} />}
        {activeTab === 'identity' && <ProspectIdentity prospect={prospect} />}
        {activeTab === 'aml' && <ProspectAML prospect={prospect} />}
        {activeTab === 'compliance' && <ProspectCompliance prospect={prospect} />}
        {activeTab === 'applicants' && <ProspectApplicants prospect={prospect} />}
      </div>
    </div>
  );
}

function ProspectOverview({ prospect, setPage }) {
  return (
    <div style={pStyles.grid3}>
      <InfoSection title="Prospect Info" items={[
        ['Prospect ID', prospect.id],
        ['Lead Ref', prospect.leadRef],
        ['Type', prospect.type],
        ['Branch', prospect.branch],
        ['Contract Type', prospect.contractType],
        ['Asset Class', prospect.asset],
        ['LoB', prospect.lob],
      ]} />
      <InfoSection title="Assignment" items={[
        ['Assignee', prospect.assignee],
        ['Stage', 'Prospect'],
        ['Created', prospect.created],
        ['Group Leasing', prospect.type==='Commercial'?'Yes':'No'],
      ]} />
      <InfoSection title="Compliance Summary" items={[
        ['PAN/GSTIN Valid', prospect.panValid ? '✓ Yes' : '✗ No'],
        ['AML Status', prospect.amlStatus],
        ['CIBIL Status', prospect.cibilStatus],
        ['Hunter Result', prospect.hunterResult==='P'?'Pass':prospect.hunterResult==='M'?'Manual':'Fail'],
        ['KYC Status', prospect.kycStatus],
      ]} />
    </div>
  );
}

function ProspectIdentity({ prospect }) {
  const steps = [
    { num:1, label:'PAN / GSTIN Validation', status:'Complete', detail: prospect.type==='Individual' ? `PAN ${prospect.pan} validated via Income Tax API · Legal name matched` : `GSTIN ${prospect.gstin} validated via GST Portal · Embedded PAN cross-verified` },
    { num:2, label:'Legal Name Enrichment', status:'Complete', detail:'Legal name and registered address fetched from government API and stored' },
    { num:3, label:'PAN Caution Gate', status:'Complete', detail:'PAN not on caution list · Allowed to proceed' },
    { num:4, label:'Passport CC Database', status:prospect.type==='Individual'?'Complete':'N/A', detail:'Passport not flagged in CC Database · No match found' },
    { num:5, label:'PAN Deduplication', status:'Complete', detail:'PAN unique · No conflict with existing contacts' },
  ];
  return (
    <div>
      <div style={pStyles.sectionHeader}>Identity Validation Steps</div>
      {steps.map((s,i) => (
        <div key={i} style={pStyles.stepRow}>
          <div style={{...pStyles.stepNum, background: s.status==='Complete'?'#69DB7C':s.status==='N/A'?'#E0E4EA':'#FFB81C', color: s.status==='N/A'?'#8896A9':'#fff'}}>{s.status==='Complete'?'✓':s.num}</div>
          <div style={{flex:1}}>
            <div style={pStyles.stepLabel}>{s.label}</div>
            <div style={pStyles.stepDetail}>{s.detail}</div>
          </div>
          <StatusChip status={s.status} />
        </div>
      ))}

      {/* PAN/GSTIN display */}
      <div style={{marginTop:20, padding:16, background:'#F8FAFC', borderRadius:10, border:'1px solid #E0E4EA'}}>
        <div style={pStyles.sectionHeader}>Validated Identifiers</div>
        <div style={pStyles.grid2}>
          {prospect.type==='Individual' ? (
            <div>
              <div style={pStyles.idFieldLabel}>PAN Number</div>
              <div style={pStyles.idFieldVal}>{prospect.pan}</div>
            </div>
          ) : (
            <div>
              <div style={pStyles.idFieldLabel}>GSTIN</div>
              <div style={pStyles.idFieldVal}>{prospect.gstin}</div>
            </div>
          )}
          <div>
            <div style={pStyles.idFieldLabel}>Constitution Type</div>
            <div style={pStyles.idFieldVal}>{prospect.type==='Individual'?'Individual':'Company'}</div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ProspectAML({ prospect }) {
  const lists = ['UNSC Al-Qaeda (1267)', 'UNSC Taliban (1988)', 'UAPA Designations', 'PEP Registry', 'Iran Sanctions (2231)', 'DPRK (1718)', 'Internal Defaulter List'];
  return (
    <div>
      <div style={{...pStyles.amlBanner, background: prospect.amlStatus==='Clear'?'#F0FFF4':'#FFF0F0', border:`1px solid ${prospect.amlStatus==='Clear'?'#69DB7C':'#FF4D4D'}`}}>
        <div style={{fontSize:22}}>{prospect.amlStatus==='Clear'?'✅':'⚠️'}</div>
        <div>
          <div style={{fontWeight:800, color: prospect.amlStatus==='Clear'?'#1A6B1A':'#8B0000', fontSize:15}}>
            AML1 — Name Screening: {prospect.amlStatus}
          </div>
          <div style={{fontSize:12, color:'#5A6A7E', marginTop:2}}>
            {prospect.amlStatus==='Clear' ? 'No match found across all sanctions lists · Screening completed' : 'Partial name match detected · Referred to AML Team · Case under investigation'}
          </div>
        </div>
      </div>

      <div style={pStyles.sectionHeader}>Sanctions Lists Screened</div>
      <div style={pStyles.listsGrid}>
        {lists.map((l,i) => (
          <div key={i} style={pStyles.listRow}>
            <span style={{color:'#69DB7C', fontSize:14}}>✓</span>
            <span style={{fontSize:12, color:'#5A6A7E'}}>{l}</span>
          </div>
        ))}
      </div>

      {prospect.amlStatus === 'Flagged' && (
        <div style={pStyles.flaggedBox}>
          <div style={pStyles.flaggedTitle}>⚠ Flagged Match Details</div>
          <div style={pStyles.flagRow}><span style={pStyles.flagKey}>Screened Name</span><span style={pStyles.flagVal}>TATA MOTORS FINANCE</span></div>
          <div style={pStyles.flagRow}><span style={pStyles.flagKey}>Matched Entry</span><span style={pStyles.flagVal}>TATA (partial) — Internal watch list cross-reference</span></div>
          <div style={pStyles.flagRow}><span style={pStyles.flagKey}>Match Type</span><span style={pStyles.flagVal}>Partial Name Match</span></div>
          <div style={pStyles.flagRow}><span style={pStyles.flagKey}>Action</span><span style={{...pStyles.flagVal, color:'#FF4D4D', fontWeight:700}}>Auto-email sent to AML Team · Awaiting investigation</span></div>
          <button style={pStyles.whitelistBtn}>Request Whitelisting →</button>
        </div>
      )}

      <div style={pStyles.amlSteps}>
        {[['AML2','PII Capture','Complete'],['AML3','Beneficial Owner Check','N/A — Individual'],['AML4','Risk Profiling','Pending'],].map(([code,label,status],i)=>(
          <div key={i} style={pStyles.amlStepRow}>
            <span style={pStyles.amlCode}>{code}</span>
            <span style={{flex:1, fontSize:12, color:'#2D3748'}}>{label}</span>
            <StatusChip status={status.includes('Complete')?'Complete':status.includes('Pending')?'Pending':'Complete'} />
          </div>
        ))}
      </div>
    </div>
  );
}

function ProspectCompliance({ prospect }) {
  return (
    <div>
      <div style={pStyles.sectionHeader}>Caution List Screening</div>
      {[
        { name:'CCDB / RBI External Caution', status:'Clear', note:'No record found in RBI CCDB' },
        { name:'UN Sanctions', status:'Clear', note:'Screened against UNSC consolidated list' },
        { name:'UAPA Designations', status:'Clear', note:'Ministry of Home Affairs list — no match' },
        { name:'Internal Defaulter / Fraud List', status:'Clear', note:'Company-internal watchlist — clear' },
        { name:'PAN Caution List', status:'Clear', note:'PAN not flagged — processing allowed' },
      ].map((c,i) => (
        <div key={i} style={pStyles.cautionRow}>
          <span style={{color:'#69DB7C', fontSize:16}}>✓</span>
          <div style={{flex:1}}>
            <div style={{fontSize:12, fontWeight:700, color:'#2D3748'}}>{c.name}</div>
            <div style={{fontSize:11, color:'#8896A9'}}>{c.note}</div>
          </div>
          <StatusChip status="Clear" />
        </div>
      ))}

      <div style={{marginTop:20}}>
        <div style={pStyles.sectionHeader}>CIBIL Bureau Request</div>
        <div style={pStyles.cibilCard}>
          <div style={{display:'flex', justifyContent:'space-between'}}>
            <div>
              <div style={{fontWeight:700, color:'#002060'}}>Bureau Request Submitted</div>
              <div style={{fontSize:11, color:'#8896A9', marginTop:2}}>Request ID: BUR-2026-04451 · Submitted: 2026-04-21</div>
            </div>
            <StatusChip status={prospect.cibilStatus} />
          </div>
          {prospect.cibilStatus==='Complete' && (
            <div style={{marginTop:12, display:'flex', gap:16}}>
              <div style={pStyles.cibilScore}><div style={pStyles.cibilScoreNum}>768</div><div style={pStyles.cibilScoreLabel}>CIBIL Score</div></div>
              <div style={{fontSize:12, color:'#5A6A7E', flex:1}}>Excellent credit history · No defaults · 7 active accounts · Utilization: 34%</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ProspectApplicants({ prospect }) {
  return (
    <div>
      <div style={pStyles.sectionHeader}>Applicants ({prospect.applicants.length})</div>
      {prospect.applicants.map((a,i) => (
        <div key={i} style={pStyles.applicantCard}>
          <div style={pStyles.applicantTop}>
            <div>
              <div style={{fontWeight:700, color:'#002060', fontSize:14}}>{a.name}</div>
              <div style={{fontSize:11, color:'#8896A9'}}>Sl. No. {a.sl} · Role: <strong>{a.role}</strong></div>
            </div>
            <div style={{display:'flex', gap:8}}>
              <AMLBadge status={a.aml} />
              <StatusChip status={a.kyc} />
            </div>
          </div>
          <div style={pStyles.applicantGrid}>
            <div><span style={pStyles.appKey}>PAN</span><span style={pStyles.appVal}>{a.pan || '—'}</span></div>
            <div><span style={pStyles.appKey}>GSTIN</span><span style={pStyles.appVal}>{a.gstin || '—'}</span></div>
            <div><span style={pStyles.appKey}>KYC</span><span style={pStyles.appVal}>{a.kyc}</span></div>
            <div><span style={pStyles.appKey}>AML</span><span style={pStyles.appVal}>{a.aml}</span></div>
          </div>
        </div>
      ))}
      <button style={{...pStyles.primaryBtn, marginTop:12, fontSize:12}}>+ Add Co-Lessee / Guarantor</button>
    </div>
  );
}

function AMLBadge({ status }) {
  const map = { Clear:['#69DB7C','#F0FFF4'], Flagged:['#FF4D4D','#FFF0F0'], 'In Screening':['#FFB81C','#FFF9E6'], Blocked:['#8B0000','#FFE5E5'] };
  const [c, bg] = map[status] || ['#ADB5BD','#F8F9FA'];
  return <span style={{padding:'2px 9px', borderRadius:20, fontSize:11, fontWeight:700, color:c, background:bg}}>🛡 {status}</span>;
}

const pStyles = {
  pageHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20 },
  stageTag: { fontSize:10, fontWeight:800, color:'#69DB7C', letterSpacing:2, marginBottom:4 },
  pageTitle: { fontSize:24, fontWeight:900, color:'#002060', letterSpacing:-0.5 },
  pageSub: { fontSize:12, color:'#8896A9', marginTop:2 },
  primaryBtn: { padding:'10px 22px', background:'#002060', color:'#FFB81C', border:'none', borderRadius:10, fontWeight:800, fontSize:13, cursor:'pointer' },
  splitView: { display:'grid', gridTemplateColumns:'300px 1fr', gap:16 },
  listPanel: { display:'flex', flexDirection:'column', gap:10, overflowY:'auto', maxHeight:'calc(100vh - 240px)' },
  card: { background:'#fff', borderRadius:12, padding:14, cursor:'pointer', border:'2px solid transparent', boxShadow:'0 1px 3px rgba(0,32,96,0.06)' },
  cardActive: { border:'2px solid #002060' },
  cardTop: { display:'flex', justifyContent:'space-between', marginBottom:8 },
  prospectName: { fontSize:13, fontWeight:700, color:'#002060' },
  prospectId: { fontSize:10, fontFamily:'monospace', color:'#8896A9', marginTop:2 },
  cardMeta: { display:'flex', gap:8, flexWrap:'wrap', marginBottom:4 },
  metaTag: { fontSize:10, background:'#F0F2F5', color:'#5A6A7E', padding:'2px 7px', borderRadius:4, fontWeight:600 },
  preferredTag: { fontSize:10, background:'#FFF9E6', color:'#E67700', padding:'2px 7px', borderRadius:4, fontWeight:700 },
  metaItem: { fontSize:10, color:'#8896A9' },
  detailPanel: { background:'#fff', borderRadius:14, padding:24, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', overflowY:'auto' },
  detailHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:16, paddingBottom:16, borderBottom:'2px solid #F0F2F5' },
  detailName: { fontSize:20, fontWeight:900, color:'#002060' },
  detailMeta: { display:'flex', gap:12, alignItems:'center', marginTop:4 },
  idChip: { fontFamily:'monospace', fontSize:11, background:'#EEF4FF', color:'#0050B3', padding:'2px 8px', borderRadius:4 },
  preferredBadge: { fontSize:10, background:'#FFF9E6', color:'#E67700', padding:'3px 10px', borderRadius:20, fontWeight:800 },
  convertBtn: { padding:'8px 16px', background:'#FFB81C', color:'#002060', border:'none', borderRadius:8, fontWeight:800, fontSize:12, cursor:'pointer' },
  complianceBar: { display:'flex', gap:0, background:'#F8FAFC', borderRadius:10, padding:'12px 16px', marginBottom:16, justifyContent:'space-between' },
  complianceItem: { display:'flex', alignItems:'center', gap:8, flex:1 },
  complianceDot: { width:10, height:10, borderRadius:5, flexShrink:0 },
  complianceLabel: { fontSize:10, color:'#8896A9', fontWeight:600 },
  complianceStatus: { fontSize:12, fontWeight:700 },
  tabBar: { display:'flex', gap:0, borderBottom:'2px solid #F0F2F5', marginBottom:16 },
  tab: { padding:'8px 16px', border:'none', background:'transparent', fontSize:11, fontWeight:700, color:'#8896A9', cursor:'pointer', letterSpacing:0.8 },
  tabActive: { color:'#002060', borderBottom:'2px solid #002060', marginBottom:-2 },
  tabContent: { minHeight:300 },
  grid3: { display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:12 },
  grid2: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 },
  sectionHeader: { fontSize:11, fontWeight:800, color:'#8896A9', letterSpacing:1.5, marginBottom:12, marginTop:4 },
  stepRow: { display:'flex', alignItems:'flex-start', gap:12, padding:'10px 0', borderBottom:'1px solid #F5F6FA' },
  stepNum: { width:24, height:24, borderRadius:12, display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700, flexShrink:0 },
  stepLabel: { fontSize:12, fontWeight:700, color:'#2D3748' },
  stepDetail: { fontSize:11, color:'#8896A9', marginTop:2 },
  idFieldLabel: { fontSize:10, color:'#8896A9', fontWeight:600, marginBottom:3 },
  idFieldVal: { fontSize:13, fontWeight:700, color:'#002060', fontFamily:'monospace' },
  amlBanner: { borderRadius:10, padding:16, display:'flex', gap:12, alignItems:'flex-start', marginBottom:16 },
  listsGrid: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:6, marginBottom:16 },
  listRow: { display:'flex', alignItems:'center', gap:8, padding:'6px 10px', background:'#F0FFF4', borderRadius:6 },
  flaggedBox: { background:'#FFF0F0', border:'1px solid #FF4D4D', borderRadius:10, padding:16, marginBottom:16 },
  flaggedTitle: { fontWeight:800, color:'#8B0000', fontSize:13, marginBottom:10 },
  flagRow: { display:'flex', gap:12, marginBottom:6 },
  flagKey: { fontSize:11, color:'#8896A9', width:110, flexShrink:0 },
  flagVal: { fontSize:11, color:'#2D3748', fontWeight:600 },
  whitelistBtn: { marginTop:10, padding:'6px 14px', background:'#002060', color:'#fff', border:'none', borderRadius:6, fontSize:11, fontWeight:700, cursor:'pointer' },
  amlSteps: { marginTop:16, background:'#F8FAFC', borderRadius:10, padding:12 },
  amlStepRow: { display:'flex', alignItems:'center', gap:12, padding:'8px 0', borderBottom:'1px solid #F0F2F5' },
  amlCode: { fontSize:10, fontWeight:800, color:'#0050B3', background:'#EEF4FF', padding:'2px 7px', borderRadius:4, width:36, textAlign:'center' },
  cautionRow: { display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderBottom:'1px solid #F5F6FA' },
  cibilCard: { background:'#F8FAFC', borderRadius:10, padding:16, border:'1px solid #E0E4EA' },
  cibilScore: { textAlign:'center' },
  cibilScoreNum: { fontSize:28, fontWeight:900, color:'#69DB7C' },
  cibilScoreLabel: { fontSize:10, color:'#8896A9', fontWeight:600 },
  applicantCard: { background:'#F8FAFC', borderRadius:10, padding:14, marginBottom:10, border:'1px solid #E0E4EA' },
  applicantTop: { display:'flex', justifyContent:'space-between', marginBottom:10 },
  applicantGrid: { display:'grid', gridTemplateColumns:'1fr 1fr 1fr 1fr', gap:8 },
  appKey: { fontSize:10, color:'#8896A9', display:'block', marginBottom:2 },
  appVal: { fontSize:12, fontWeight:700, color:'#2D3748' },
};

Object.assign(window, { ProspectsPage, AMLBadge, pStyles });
