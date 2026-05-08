
// ─── Lead Management ──────────────────────────────────────────────────────────

function LeadsPage({ role, setPage }) {
  const [selected, setSelected] = React.useState(null);
  const [showCreate, setShowCreate] = React.useState(false);
  const [filter, setFilter] = React.useState('All');

  const filters = ['All','Hot','Warm','Cold'];
  const leads = filter === 'All' ? window.SF_LEADS : window.SF_LEADS.filter(l => l.status === filter);
  const lead = selected ? window.SF_LEADS.find(l => l.id === selected) : null;

  if (showCreate) return <CreateLeadForm onBack={() => setShowCreate(false)} />;

  return (
    <div>
      <div style={leadStyles.pageHeader}>
        <div>
          <div style={leadStyles.stageTag}>① STAGE 1</div>
          <div style={leadStyles.pageTitle}>Lead Generation</div>
          <div style={leadStyles.pageSub}>Capture and qualify prospective lessees · {window.SF_LEADS.length} total leads</div>
        </div>
        <button style={leadStyles.primaryBtn} onClick={() => setShowCreate(true)}>+ Create Lead</button>
      </div>

      {/* Filter tabs */}
      <div style={leadStyles.filterRow}>
        {filters.map(f => (
          <button key={f} style={{...leadStyles.filterTab, ...(filter===f ? leadStyles.filterTabActive : {})}}
            onClick={() => setFilter(f)}>{f}
            {f!=='All' && <span style={{marginLeft:5, fontSize:10, opacity:0.7}}>
              {window.SF_LEADS.filter(l=>l.status===f).length}
            </span>}
          </button>
        ))}
        <div style={{marginLeft:'auto', fontSize:11, color:'#8896A9', display:'flex', alignItems:'center', gap:6}}>
          <span>Sort: Latest First</span>
        </div>
      </div>

      <div style={leadStyles.splitView}>
        {/* Lead List */}
        <div style={leadStyles.listPanel}>
          {leads.map(l => (
            <div key={l.id} style={{...leadStyles.leadCard, ...(selected===l.id ? leadStyles.leadCardActive : {})}}
              onClick={() => setSelected(l.id)}>
              <div style={leadStyles.leadCardTop}>
                <div>
                  <div style={leadStyles.leadName}>{l.name}</div>
                  <div style={leadStyles.leadId}>{l.id}</div>
                </div>
                <StatusChip status={l.status} />
              </div>
              <div style={leadStyles.leadMeta}>
                <span style={{...leadStyles.typePill, background: l.type==='Individual'?'#EEF6FF':'#FFF4E6', color: l.type==='Individual'?'#0050B3':'#E67700'}}>{l.type}</span>
                <span style={leadStyles.metaItem}>📍 {l.asset}</span>
                <span style={leadStyles.metaItem}>👤 {l.assignee}</span>
              </div>
              <div style={leadStyles.leadFooter}>
                <DedupChip label={l.dedup} />
                <span style={leadStyles.metaItem}>{l.source}</span>
                <span style={leadStyles.metaItem}>{l.created}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Lead Detail */}
        {lead ? (
          <div style={leadStyles.detailPanel}>
            <div style={leadStyles.detailHeader}>
              <div>
                <div style={leadStyles.detailName}>{lead.name}</div>
                <div style={leadStyles.detailId}>{lead.id}</div>
              </div>
              <div style={{display:'flex', gap:8, alignItems:'center'}}>
                <StatusChip status={lead.status} />
                <button style={leadStyles.promoteBtn} onClick={() => setPage('prospects')}>
                  Promote to Prospect →
                </button>
              </div>
            </div>

            <div style={leadStyles.detailGrid}>
              <InfoSection title="Identity" items={[
                ['Lead Type', lead.type],
                ['PAN', lead.pan || '—'],
                ['GSTIN', lead.gstin || '—'],
                ['Mobile', lead.mobile],
                ['Dedup Status', <DedupChip key="d" label={lead.dedup} />],
              ]} />
              <InfoSection title="Lead Info" items={[
                ['Source Category', lead.source],
                ['Asset Interest', lead.asset],
                ['Stage', lead.stage],
                ['Created', lead.created],
              ]} />
              <InfoSection title="Assignment" items={[
                ['Assignee', lead.assignee || 'Unassigned'],
                ['Branch', 'Chennai Main'],
                ['LoB', 'Leasing'],
              ]} />
            </div>

            {/* Interaction log */}
            <div style={leadStyles.section}>
              <div style={leadStyles.sectionTitle}>Interaction Log</div>
              {[
                { type:'Call', date:'2026-04-23 10:30', outcome:'Prospect expressed strong interest in 3-year finance lease for commercial vehicle.', next:'Meeting on 25-Apr' },
                { type:'Email', date:'2026-04-21 14:00', outcome:'Sent product brochure and rack rate overview.', next:'Follow-up call' },
              ].map((i, idx) => (
                <div key={idx} style={leadStyles.interactionRow}>
                  <div style={{...leadStyles.interactionIcon, background: i.type==='Call'?'#EEF6FF':'#FFF4E6', color: i.type==='Call'?'#0050B3':'#E67700'}}>
                    {i.type==='Call'?'📞':'✉️'}
                  </div>
                  <div>
                    <div style={leadStyles.interactionHead}>{i.type} · <span style={{color:'#8896A9'}}>{i.date}</span></div>
                    <div style={leadStyles.interactionBody}>{i.outcome}</div>
                    <div style={leadStyles.interactionNext}>Next: {i.next}</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Qualification */}
            <div style={leadStyles.section}>
              <div style={leadStyles.sectionTitle}>Qualification Status</div>
              <div style={leadStyles.qualRow}>
                {['Cold','Warm','Hot'].map(q => (
                  <div key={q} style={{...leadStyles.qualBtn, ...(lead.status===q ? {background:'#002060', color:'#FFB81C', borderColor:'#002060'} : {})}}>
                    {q}
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div style={leadStyles.emptyDetail}>
            <div style={{fontSize:40, opacity:0.2}}>⟡</div>
            <div style={{color:'#8896A9', fontSize:13, marginTop:8}}>Select a lead to view details</div>
          </div>
        )}
      </div>
    </div>
  );
}

function CreateLeadForm({ onBack }) {
  const [step, setStep] = React.useState(1);
  const [form, setForm] = React.useState({ type:'Individual', name:'', mobile:'', pan:'', source:'Dealer', asset:'', email:'' });
  const set = (k,v) => setForm(p => ({...p, [k]:v}));

  return (
    <div>
      <div style={leadStyles.pageHeader}>
        <div>
          <button style={leadStyles.backBtn} onClick={onBack}>← Back to Leads</button>
          <div style={leadStyles.pageTitle}>Create New Lead</div>
        </div>
        <StepIndicator steps={['Basic Info','Identity','Source & Asset','Review']} current={step} />
      </div>

      <div style={leadStyles.formCard}>
        {step === 1 && (
          <div>
            <FormSection title="Lead Type">
              <div style={{display:'flex', gap:12}}>
                {['Individual','Commercial'].map(t => (
                  <div key={t} style={{...leadStyles.typeCard, ...(form.type===t ? leadStyles.typeCardActive : {})}}
                    onClick={() => set('type',t)}>
                    <div style={{fontSize:22}}>{t==='Individual'?'👤':'🏢'}</div>
                    <div style={{fontWeight:700, fontSize:13, marginTop:6}}>{t}</div>
                    <div style={{fontSize:11, color:'#8896A9', marginTop:2}}>
                      {t==='Individual' ? 'Natural person' : 'Company / Group fleet'}
                    </div>
                  </div>
                ))}
              </div>
            </FormSection>
            <FormSection title="Contact Name *">
              <input style={leadStyles.input} placeholder="Full name" value={form.name} onChange={e=>set('name',e.target.value)} />
            </FormSection>
            <FormSection title="Mobile Number *">
              <input style={leadStyles.input} placeholder="10-digit mobile" value={form.mobile} onChange={e=>set('mobile',e.target.value)} />
            </FormSection>
            <FormSection title="Email ID">
              <input style={leadStyles.input} placeholder="email@company.com" value={form.email} onChange={e=>set('email',e.target.value)} />
            </FormSection>
          </div>
        )}
        {step === 2 && (
          <div>
            <FormSection title={form.type==='Individual' ? 'PAN Number (optional at this stage)' : 'GSTIN (optional at this stage)'}>
              <input style={leadStyles.input} placeholder={form.type==='Individual'?'ABCDE1234F':'29AABCI1234C1Z5'} value={form.pan} onChange={e=>set('pan',e.target.value)} />
              <div style={leadStyles.fieldNote}>⚠ PAN / GSTIN becomes mandatory at Prospect promotion stage</div>
            </FormSection>
            {form.pan && (
              <div style={leadStyles.dedupResult}>
                <div style={{fontWeight:700, color:'#69DB7C'}}>✓ Dedup Check: New</div>
                <div style={{fontSize:11, color:'#8896A9', marginTop:2}}>No existing customer found for this PAN</div>
              </div>
            )}
          </div>
        )}
        {step === 3 && (
          <div>
            <FormSection title="Source Category *">
              <select style={leadStyles.input} value={form.source} onChange={e=>set('source',e.target.value)}>
                {['Dealer','Direct','Campaign','Internal','Mobile App','API'].map(s=><option key={s}>{s}</option>)}
              </select>
            </FormSection>
            <FormSection title="Source Name">
              <input style={leadStyles.input} placeholder="e.g. Diwali-CAR-LEASE-2026" />
            </FormSection>
            <FormSection title="Asset Category *">
              <select style={leadStyles.input} value={form.asset} onChange={e=>set('asset',e.target.value)}>
                <option value="">Select asset category</option>
                {['Passenger Vehicle','Commercial Vehicle','IT Equipment','Medical Equipment','Construction Equipment','Office Equipment'].map(a=><option key={a}>{a}</option>)}
              </select>
            </FormSection>
            <FormSection title="Contract Type">
              <div style={{display:'flex', gap:10}}>
                {['Finance Lease','Operating Lease'].map(t=>(
                  <div key={t} style={{...leadStyles.radioCard}} onClick={()=>set('contractType',t)}>
                    <span style={{fontSize:12, fontWeight:600, color: form.contractType===t?'#002060':'#5A6A7E'}}>{t}</span>
                  </div>
                ))}
              </div>
            </FormSection>
          </div>
        )}
        {step === 4 && (
          <div>
            <div style={leadStyles.reviewBanner}>
              <div style={{fontSize:18, marginBottom:8}}>✅ Review Lead Details</div>
              <div style={{fontSize:12, color:'#5A6A7E'}}>System will generate <strong>Temp Customer No.</strong> and <strong>Lead Ref No.</strong> on save</div>
            </div>
            {[['Name', form.name||'—'],['Type',form.type],['Mobile',form.mobile||'—'],['PAN/GSTIN',form.pan||'Not provided'],['Source',form.source],['Asset',form.asset||'—']].map(([k,v],i)=>(
              <div key={i} style={leadStyles.reviewRow}><span style={leadStyles.reviewKey}>{k}</span><span style={leadStyles.reviewVal}>{v}</span></div>
            ))}
            <div style={{marginTop:12, padding:12, background:'#FFF9E6', borderRadius:8, fontSize:11, color:'#7A5200'}}>
              ⚡ Assignment: Will be auto-assigned to CPU queue. Default SLA: 4 hours first contact.
            </div>
          </div>
        )}

        <div style={leadStyles.formFooter}>
          {step > 1 && <button style={leadStyles.secondaryBtn} onClick={() => setStep(s=>s-1)}>← Back</button>}
          {step < 4 ? (
            <button style={leadStyles.primaryBtn} onClick={() => setStep(s=>s+1)}>Continue →</button>
          ) : (
            <button style={{...leadStyles.primaryBtn, background:'#69DB7C', color:'#003300'}} onClick={onBack}>
              ✓ Save Lead
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function StepIndicator({ steps, current }) {
  return (
    <div style={{display:'flex', alignItems:'center', gap:0}}>
      {steps.map((s, i) => {
        const n = i+1;
        const done = n < current, active = n === current;
        return (
          <React.Fragment key={i}>
            <div style={{display:'flex', flexDirection:'column', alignItems:'center', gap:3}}>
              <div style={{width:26, height:26, borderRadius:13, background: done?'#69DB7C': active?'#002060':'#E0E4EA', color: done||active?'#fff':'#8896A9', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700}}>
                {done ? '✓' : n}
              </div>
              <div style={{fontSize:9, color: active?'#002060':'#8896A9', fontWeight: active?700:400, whiteSpace:'nowrap'}}>{s}</div>
            </div>
            {i < steps.length-1 && <div style={{width:30, height:2, background: done?'#69DB7C':'#E0E4EA', marginBottom:14, flexShrink:0}} />}
          </React.Fragment>
        );
      })}
    </div>
  );
}

function FormSection({ title, children }) {
  return <div style={{marginBottom:18}}><label style={{display:'block', fontSize:11, fontWeight:700, color:'#5A6A7E', marginBottom:6, letterSpacing:0.5}}>{title}</label>{children}</div>;
}

function InfoSection({ title, items }) {
  return (
    <div style={leadStyles.infoBox}>
      <div style={leadStyles.infoTitle}>{title}</div>
      {items.map(([k,v],i) => (
        <div key={i} style={leadStyles.infoRow}>
          <span style={leadStyles.infoKey}>{k}</span>
          <span style={leadStyles.infoVal}>{v}</span>
        </div>
      ))}
    </div>
  );
}

const leadStyles = {
  pageHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20 },
  stageTag: { fontSize:10, fontWeight:800, color:'#4DABF7', letterSpacing:2, marginBottom:4 },
  pageTitle: { fontSize:24, fontWeight:900, color:'#002060', letterSpacing:-0.5 },
  pageSub: { fontSize:12, color:'#8896A9', marginTop:2 },
  primaryBtn: { padding:'10px 22px', background:'#002060', color:'#FFB81C', border:'none', borderRadius:10, fontWeight:800, fontSize:13, cursor:'pointer' },
  secondaryBtn: { padding:'10px 22px', background:'#F0F2F5', color:'#5A6A7E', border:'none', borderRadius:10, fontWeight:700, fontSize:13, cursor:'pointer' },
  backBtn: { fontSize:12, color:'#0050B3', background:'none', border:'none', cursor:'pointer', fontWeight:600, marginBottom:6, display:'block' },
  filterRow: { display:'flex', gap:8, marginBottom:16, alignItems:'center' },
  filterTab: { padding:'6px 16px', borderRadius:20, border:'1px solid #E0E4EA', background:'#fff', fontSize:12, color:'#5A6A7E', cursor:'pointer', fontWeight:600 },
  filterTabActive: { background:'#002060', color:'#FFB81C', borderColor:'#002060', fontWeight:800 },
  splitView: { display:'grid', gridTemplateColumns:'340px 1fr', gap:16 },
  listPanel: { overflowY:'auto', maxHeight:'calc(100vh - 250px)', display:'flex', flexDirection:'column', gap:10 },
  leadCard: { background:'#fff', borderRadius:12, padding:14, cursor:'pointer', border:'2px solid transparent', boxShadow:'0 1px 3px rgba(0,32,96,0.06)', transition:'all 0.15s' },
  leadCardActive: { border:'2px solid #002060', boxShadow:'0 4px 12px rgba(0,32,96,0.12)' },
  leadCardTop: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:8 },
  leadName: { fontSize:13, fontWeight:700, color:'#002060' },
  leadId: { fontSize:10, color:'#8896A9', fontFamily:'monospace', marginTop:2 },
  leadMeta: { display:'flex', gap:8, alignItems:'center', marginBottom:6, flexWrap:'wrap' },
  leadFooter: { display:'flex', gap:8, alignItems:'center', flexWrap:'wrap' },
  typePill: { padding:'2px 8px', borderRadius:4, fontSize:10, fontWeight:600 },
  metaItem: { fontSize:10, color:'#8896A9' },
  detailPanel: { background:'#fff', borderRadius:14, padding:24, boxShadow:'0 1px 4px rgba(0,32,96,0.07)' },
  emptyDetail: { background:'#fff', borderRadius:14, display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', minHeight:300, boxShadow:'0 1px 4px rgba(0,32,96,0.07)' },
  detailHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20, paddingBottom:16, borderBottom:'2px solid #F0F2F5' },
  detailName: { fontSize:20, fontWeight:900, color:'#002060' },
  detailId: { fontSize:11, color:'#8896A9', fontFamily:'monospace', marginTop:2 },
  promoteBtn: { padding:'8px 16px', background:'#FFB81C', color:'#002060', border:'none', borderRadius:8, fontWeight:800, fontSize:12, cursor:'pointer' },
  detailGrid: { display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:12, marginBottom:20 },
  infoBox: { background:'#F8FAFC', borderRadius:10, padding:14 },
  infoTitle: { fontSize:10, fontWeight:800, color:'#8896A9', letterSpacing:1.5, marginBottom:10 },
  infoRow: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:7 },
  infoKey: { fontSize:11, color:'#8896A9' },
  infoVal: { fontSize:11, fontWeight:600, color:'#2D3748' },
  section: { marginTop:20 },
  sectionTitle: { fontSize:12, fontWeight:800, color:'#002060', marginBottom:10, letterSpacing:0.3 },
  interactionRow: { display:'flex', gap:12, padding:'10px 0', borderBottom:'1px solid #F5F6FA' },
  interactionIcon: { width:34, height:34, borderRadius:8, display:'flex', alignItems:'center', justifyContent:'center', fontSize:16, flexShrink:0 },
  interactionHead: { fontSize:12, fontWeight:700, color:'#2D3748' },
  interactionBody: { fontSize:11, color:'#5A6A7E', marginTop:3 },
  interactionNext: { fontSize:10, color:'#FFB81C', marginTop:3, fontWeight:600 },
  qualRow: { display:'flex', gap:10 },
  qualBtn: { padding:'6px 18px', borderRadius:20, border:'1px solid #E0E4EA', fontSize:12, fontWeight:600, color:'#5A6A7E', cursor:'pointer' },
  formCard: { background:'#fff', borderRadius:14, padding:28, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', maxWidth:700 },
  typeCard: { flex:1, border:'2px solid #E0E4EA', borderRadius:12, padding:16, cursor:'pointer', textAlign:'center', transition:'all 0.15s' },
  typeCardActive: { border:'2px solid #002060', background:'#EEF4FF' },
  radioCard: { flex:1, border:'1px solid #E0E4EA', borderRadius:8, padding:'10px 14px', cursor:'pointer', textAlign:'center' },
  input: { width:'100%', padding:'10px 14px', border:'1px solid #E0E4EA', borderRadius:8, fontSize:13, outline:'none', boxSizing:'border-box', color:'#2D3748', background:'#FAFBFC' },
  fieldNote: { fontSize:10, color:'#FFB81C', marginTop:5, fontWeight:600 },
  dedupResult: { padding:14, background:'#F0FFF4', borderRadius:8, border:'1px solid #69DB7C' },
  reviewBanner: { background:'#EEF4FF', borderRadius:10, padding:16, marginBottom:16, textAlign:'center', color:'#002060', fontWeight:700 },
  reviewRow: { display:'flex', justifyContent:'space-between', padding:'8px 0', borderBottom:'1px solid #F0F2F5' },
  reviewKey: { fontSize:12, color:'#8896A9' },
  reviewVal: { fontSize:12, fontWeight:700, color:'#2D3748' },
  formFooter: { display:'flex', justifyContent:'flex-end', gap:10, marginTop:24, paddingTop:16, borderTop:'1px solid #F0F2F5' },
};

Object.assign(window, { LeadsPage, StepIndicator, FormSection, InfoSection, leadStyles });
