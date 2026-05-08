
// ─── Opportunities + Quotes ───────────────────────────────────────────────────

function OpportunitiesPage({ role, setPage }) {
  const [selected, setSelected] = React.useState('OPP-2026-00031');
  const opp = window.SF_OPPORTUNITIES.find(o => o.id === selected);

  return (
    <div>
      <div style={oqStyles.pageHeader}>
        <div>
          <div style={oqStyles.stageTag}>③ STAGE 3</div>
          <div style={oqStyles.pageTitle}>Opportunity Management</div>
          <div style={oqStyles.pageSub}>Asset interest per Line of Business · {window.SF_OPPORTUNITIES.length} active opportunities</div>
        </div>
        <button style={oqStyles.primaryBtn}>+ New Opportunity</button>
      </div>

      <div style={oqStyles.splitView}>
        <div style={oqStyles.listPanel}>
          {window.SF_OPPORTUNITIES.map(o => (
            <div key={o.id} style={{...oqStyles.card, ...(selected===o.id?oqStyles.cardActive:{})}}
              onClick={() => setSelected(o.id)}>
              <div style={oqStyles.cardTop}>
                <div>
                  <div style={oqStyles.oppName}>{o.prospectName}</div>
                  <div style={oqStyles.oppId}>{o.id}</div>
                </div>
                <StatusChip status={o.status} />
              </div>
              <div style={oqStyles.cardMeta}>
                <span style={oqStyles.lobTag}>{o.lob}</span>
                <span style={oqStyles.metaItem}>{o.assetClass}</span>
              </div>
              <div style={oqStyles.cardMeta}>
                <span style={oqStyles.metaItem}>Volume: <strong>{o.volume} units</strong></span>
                <span style={oqStyles.metaItem}>Value: <strong>₹{(o.value/1000000).toFixed(1)}M</strong></span>
                {o.preferred && <span style={oqStyles.prefTag}>⭐ Preferred</span>}
              </div>
            </div>
          ))}
        </div>

        {opp && (
          <div style={oqStyles.detailPanel}>
            <div style={oqStyles.detailHeader}>
              <div>
                <div style={oqStyles.detailName}>{opp.prospectName}</div>
                <div style={oqStyles.detailMeta}>
                  <span style={oqStyles.idChip}>{opp.id}</span>
                  <span style={oqStyles.metaItem}>Prospect: {opp.prospectId}</span>
                </div>
              </div>
              <div style={{display:'flex', gap:8, alignItems:'center'}}>
                <StatusChip status={opp.status} />
                <button style={oqStyles.quoteBtn} onClick={() => setPage('quotes')}>
                  {opp.quoteId ? `View Quote ${opp.quoteId} →` : 'Generate Quote →'}
                </button>
              </div>
            </div>

            <div style={oqStyles.grid3}>
              <InfoSection title="Opportunity Details" items={[
                ['Opportunity ID', opp.id],
                ['Line of Business', opp.lob],
                ['Asset Category', opp.assetCategory],
                ['Asset Class', opp.assetClass],
              ]} />
              <InfoSection title="Business Potential" items={[
                ['Volume (Units)', opp.volume],
                ['Potential Value', `₹${(opp.value/1000000).toFixed(2)}M`],
                ['Preferred Rate', opp.preferred ? 'Yes — Rack Rate applies' : 'Standard Rate'],
              ]} />
              <InfoSection title="Quote Status" items={[
                ['Quote ID', opp.quoteId || '—'],
                ['Opp. Status', opp.status],
              ]} />
            </div>

            {/* Lifecycle */}
            <div style={{marginTop:20}}>
              <div style={oqStyles.sectionTitle}>Opportunity Lifecycle</div>
              <div style={oqStyles.lifecycle}>
                {['Open','Quoted','Negotiation','QuoteLocked','Converted'].map((s,i) => {
                  const stages = ['Open','Quoted','Negotiation','QuoteLocked','Converted'];
                  const curIdx = stages.indexOf(opp.status);
                  const thisIdx = stages.indexOf(s);
                  const done = thisIdx < curIdx, active = thisIdx === curIdx;
                  return (
                    <React.Fragment key={i}>
                      <div style={oqStyles.lcStep}>
                        <div style={{...oqStyles.lcDot, background: done?'#69DB7C': active?'#002060':'#E0E4EA', color: done||active?'#fff':'#8896A9'}}>
                          {done ? '✓' : i+1}
                        </div>
                        <div style={{...oqStyles.lcLabel, color: active?'#002060': done?'#69DB7C':'#8896A9'}}>{s}</div>
                      </div>
                      {i < 4 && <div style={{...oqStyles.lcLine, background: done?'#69DB7C':'#E0E4EA'}} />}
                    </React.Fragment>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Pricing & Quote ─────────────────────────────────────────────────────────

function QuotesPage({ role }) {
  const [view, setView] = React.useState('list'); // 'list' | 'builder' | 'rack'
  const [selectedQ, setSelectedQ] = React.useState('Q-202600013');
  const q = window.SF_QUOTES.find(x => x.id === selectedQ);

  if (view === 'rack') return <RackRatePage onBack={() => setView('list')} />;

  return (
    <div>
      <div style={oqStyles.pageHeader}>
        <div>
          <div style={oqStyles.stageTagQuote}>④ STAGE 4</div>
          <div style={oqStyles.pageTitle}>Pricing & Quote Management</div>
          <div style={oqStyles.pageSub}>Rack rates · Quote generation · Negotiation · Authority matrix</div>
        </div>
        <div style={{display:'flex', gap:10}}>
          <button style={oqStyles.secondaryBtn} onClick={() => setView('rack')}>📋 Rack Rate Master</button>
          <button style={oqStyles.primaryBtn}>+ New Quote</button>
        </div>
      </div>

      <div style={oqStyles.splitView}>
        <div style={oqStyles.listPanel}>
          {window.SF_QUOTES.map(q => (
            <div key={q.id} style={{...oqStyles.card, ...(selectedQ===q.id?oqStyles.cardActive:{})}}
              onClick={() => setSelectedQ(q.id)}>
              <div style={oqStyles.cardTop}>
                <div>
                  <div style={oqStyles.oppName}>{q.prospectName}</div>
                  <div style={oqStyles.oppId}>{q.id}</div>
                </div>
                <StatusChip status={q.status} />
              </div>
              <div style={oqStyles.cardMeta}>
                <span style={oqStyles.metaItem}>{q.assetClass}</span>
                <span style={oqStyles.metaItem}>{q.leaseType}</span>
              </div>
              <div style={oqStyles.cardMeta}>
                <span style={oqStyles.metaItem}>Rack: {q.rackRate}</span>
                <span style={{...oqStyles.varianceBadge, color: q.variance<0?'#FF4D4D':'#69DB7C', background: q.variance<0?'#FFF0F0':'#F0FFF4'}}>
                  {q.variance > 0 ? '+' : ''}{q.variance.toFixed(2)}%
                </span>
              </div>
            </div>
          ))}
        </div>

        {q && <QuoteDetail quote={q} role={role} />}
      </div>
    </div>
  );
}

function QuoteDetail({ quote, role }) {
  const absVar = Math.abs(quote.variance);
  const varColor = absVar <= 2 ? '#69DB7C' : absVar <= 5 ? '#FFB81C' : '#FF4D4D';

  return (
    <div style={oqStyles.detailPanel}>
      <div style={oqStyles.detailHeader}>
        <div>
          <div style={oqStyles.detailName}>{quote.prospectName}</div>
          <div style={oqStyles.detailMeta}>
            <span style={oqStyles.idChip}>{quote.id}</span>
            <span style={oqStyles.metaItem}>Rack: {quote.rackRate}</span>
            <span style={oqStyles.metaItem}>v{quote.version}</span>
          </div>
        </div>
        <div style={{display:'flex', gap:8, alignItems:'center'}}>
          <StatusChip status={quote.status} />
          {quote.status !== 'Locked' && <button style={oqStyles.quoteBtn}>Lock Quote →</button>}
        </div>
      </div>

      {/* Pricing Summary */}
      <div style={oqStyles.pricingGrid}>
        {[
          { label:'Asset Cost', val:`₹${(quote.assetCost/1000000).toFixed(2)}M`, highlight:false },
          { label:'Lease Type', val: quote.leaseType, highlight:false },
          { label:'Tenure', val:`${quote.tenure} months`, highlight:false },
          { label:'Frequency', val: quote.frequency==='M'?'Monthly':'Quarterly', highlight:false },
          { label:'Rack Rental', val:`₹${quote.rackRental.toLocaleString()}`, highlight:false },
          { label:'Proposed Rental', val:`₹${quote.proposedRental.toLocaleString()}`, highlight:true },
        ].map((p,i) => (
          <div key={i} style={{...oqStyles.pricingCard, ...(p.highlight ? oqStyles.pricingCardHighlight : {})}}>
            <div style={oqStyles.pricingLabel}>{p.label}</div>
            <div style={{...oqStyles.pricingVal, ...(p.highlight ? {color:'#002060', fontSize:22} : {})}}>{p.val}</div>
          </div>
        ))}
      </div>

      {/* Variance & Authority */}
      <div style={oqStyles.varianceBox}>
        <div style={oqStyles.varianceRow}>
          <div>
            <div style={oqStyles.varLabel}>Rental Variance vs Rack Rate</div>
            <div style={{...oqStyles.varVal, color: varColor}}>{quote.variance.toFixed(2)}%</div>
          </div>
          <div style={oqStyles.varDivider} />
          <div>
            <div style={oqStyles.varLabel}>Approval Required</div>
            <div style={{...oqStyles.varVal, fontSize:14, color: absVar>5?'#FF4D4D':'#FFB81C'}}>{quote.approvalTier}</div>
          </div>
          <div style={oqStyles.varDivider} />
          <div>
            <div style={oqStyles.varLabel}>Approval Status</div>
            <StatusChip status={quote.approvalStatus} />
          </div>
          <div style={oqStyles.varDivider} />
          <div>
            <div style={oqStyles.varLabel}>Valid Until</div>
            <div style={oqStyles.varVal}>{quote.validTo}</div>
          </div>
        </div>
        {quote.negotiationReason && (
          <div style={oqStyles.reasonRow}>
            <span style={oqStyles.reasonLabel}>Negotiation Reason:</span>
            <span style={oqStyles.reasonVal}>{quote.negotiationReason}</span>
          </div>
        )}
      </div>

      {/* Authority Matrix */}
      <div style={{marginTop:16}}>
        <div style={oqStyles.sectionTitle}>Authority Matrix — Rental Variance</div>
        <table style={oqStyles.matrixTable}>
          <thead>
            <tr>{['Variance Band','Approval Level','Status'].map((h,i)=>
              <th key={i} style={oqStyles.matrixTh}>{h}</th>
            )}</tr>
          </thead>
          <tbody>
            {[
              ['≤ 2%','Auto-Approve', absVar<=2],
              ['> 2% and ≤ 5%','Manager Approval', absVar>2&&absVar<=5],
              ['> 5% and ≤ 10%','Pricing Head', absVar>5&&absVar<=10],
              ['> 10%','Finance / Policy Committee', absVar>10],
            ].map(([band,level,active],i) => (
              <tr key={i} style={active?{background:'#FFF9E6'}:{}}>
                <td style={oqStyles.matrixTd}>{band}</td>
                <td style={oqStyles.matrixTd}>{level}</td>
                <td style={oqStyles.matrixTd}>{active ? <span style={{color:'#FFB81C',fontWeight:800}}>← CURRENT</span> : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Quote lifecycle */}
      <div style={{marginTop:16}}>
        <div style={oqStyles.sectionTitle}>Quote Lifecycle</div>
        <div style={oqStyles.lifecycle}>
          {['Draft','Negotiation','PendingApproval','Approved','Locked'].map((s,i) => {
            const stages = ['Draft','Negotiation','PendingApproval','Approved','Locked'];
            const curIdx = stages.indexOf(quote.status==='Locked'?'Locked':quote.status==='Negotiation'?'Negotiation':quote.approvalStatus==='Approved'?'Approved':quote.approvalStatus==='Pending'?'PendingApproval':'Draft');
            const done = i < curIdx, active = i === curIdx;
            return (
              <React.Fragment key={i}>
                <div style={oqStyles.lcStep}>
                  <div style={{...oqStyles.lcDot, background: done?'#69DB7C': active?'#DA77F2':'#E0E4EA', color: done||active?'#fff':'#8896A9'}}>
                    {done?'✓':i+1}
                  </div>
                  <div style={{...oqStyles.lcLabel, color: active?'#002060':done?'#69DB7C':'#8896A9'}}>{s}</div>
                </div>
                {i<4 && <div style={{...oqStyles.lcLine, background: done?'#69DB7C':'#E0E4EA'}} />}
              </React.Fragment>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function RackRatePage({ onBack }) {
  return (
    <div>
      <div style={oqStyles.pageHeader}>
        <div>
          <button style={oqStyles.backBtn} onClick={onBack}>← Back to Quotes</button>
          <div style={oqStyles.pageTitle}>Rack Rate Master</div>
          <div style={oqStyles.pageSub}>Approved pricing templates for all asset classes</div>
        </div>
        {/* Maker-Checker badge */}
        <div style={oqStyles.makerCheckerBadge}>🔒 Maker-Checker Enforced</div>
      </div>
      <table style={{width:'100%', borderCollapse:'collapse', background:'#fff', borderRadius:14, overflow:'hidden', boxShadow:'0 1px 4px rgba(0,32,96,0.07)'}}>
        <thead>
          <tr>{['Rack Code','Asset Class','Lease Type','Tenure','CoF%','GST%','Dep%','Residual%','LMF%','Deposit%','ATDR%','Status','Effective'].map((h,i)=>
            <th key={i} style={oqStyles.matrixTh}>{h}</th>
          )}</tr>
        </thead>
        <tbody>
          {window.SF_RACK_RATES.map((r,i) => (
            <tr key={i} style={i%2===0?{}:{background:'#F8FAFC'}}>
              <td style={{...oqStyles.matrixTd, fontFamily:'monospace', color:'#0050B3', fontWeight:700}}>{r.id}</td>
              <td style={oqStyles.matrixTd}>{r.assetClass}</td>
              <td style={oqStyles.matrixTd}>{r.leaseType}</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.tenure}M</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.cof}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.gst}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.depRate}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.residualPct}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.lmf}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.deposit}%</td>
              <td style={{...oqStyles.matrixTd, textAlign:'center'}}>{r.atdr}%</td>
              <td style={oqStyles.matrixTd}><StatusChip status={r.status} /></td>
              <td style={{...oqStyles.matrixTd, fontSize:10}}>{r.effectiveFrom} → {r.effectiveTo}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const oqStyles = {
  pageHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20 },
  stageTag: { fontSize:10, fontWeight:800, color:'#FFA94D', letterSpacing:2, marginBottom:4 },
  stageTagQuote: { fontSize:10, fontWeight:800, color:'#DA77F2', letterSpacing:2, marginBottom:4 },
  pageTitle: { fontSize:24, fontWeight:900, color:'#002060', letterSpacing:-0.5 },
  pageSub: { fontSize:12, color:'#8896A9', marginTop:2 },
  primaryBtn: { padding:'10px 22px', background:'#002060', color:'#FFB81C', border:'none', borderRadius:10, fontWeight:800, fontSize:13, cursor:'pointer' },
  secondaryBtn: { padding:'10px 22px', background:'#F0F2F5', color:'#5A6A7E', border:'none', borderRadius:10, fontWeight:700, fontSize:12, cursor:'pointer' },
  backBtn: { fontSize:12, color:'#0050B3', background:'none', border:'none', cursor:'pointer', fontWeight:600, marginBottom:6, display:'block' },
  splitView: { display:'grid', gridTemplateColumns:'300px 1fr', gap:16 },
  listPanel: { display:'flex', flexDirection:'column', gap:10 },
  card: { background:'#fff', borderRadius:12, padding:14, cursor:'pointer', border:'2px solid transparent', boxShadow:'0 1px 3px rgba(0,32,96,0.06)' },
  cardActive: { border:'2px solid #002060' },
  cardTop: { display:'flex', justifyContent:'space-between', marginBottom:8 },
  oppName: { fontSize:13, fontWeight:700, color:'#002060' },
  oppId: { fontSize:10, fontFamily:'monospace', color:'#8896A9', marginTop:2 },
  cardMeta: { display:'flex', gap:8, flexWrap:'wrap', marginBottom:4 },
  lobTag: { fontSize:10, background:'#EEF4FF', color:'#0050B3', padding:'2px 8px', borderRadius:4, fontWeight:600 },
  prefTag: { fontSize:10, background:'#FFF9E6', color:'#E67700', padding:'2px 7px', borderRadius:4, fontWeight:700 },
  metaItem: { fontSize:10, color:'#8896A9' },
  varianceBadge: { fontSize:10, fontWeight:700, padding:'2px 7px', borderRadius:4 },
  detailPanel: { background:'#fff', borderRadius:14, padding:24, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', overflowY:'auto' },
  detailHeader: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:20, paddingBottom:16, borderBottom:'2px solid #F0F2F5' },
  detailName: { fontSize:20, fontWeight:900, color:'#002060' },
  detailMeta: { display:'flex', gap:12, alignItems:'center', marginTop:4 },
  idChip: { fontFamily:'monospace', fontSize:11, background:'#EEF4FF', color:'#0050B3', padding:'2px 8px', borderRadius:4 },
  quoteBtn: { padding:'8px 16px', background:'#FFB81C', color:'#002060', border:'none', borderRadius:8, fontWeight:800, fontSize:12, cursor:'pointer' },
  pricingGrid: { display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12, marginBottom:16 },
  pricingCard: { background:'#F8FAFC', borderRadius:10, padding:14, border:'1px solid #E0E4EA' },
  pricingCardHighlight: { background:'#002060', border:'1px solid #002060' },
  pricingLabel: { fontSize:10, color:'#8896A9', fontWeight:600, marginBottom:4 },
  pricingVal: { fontSize:16, fontWeight:800, color:'#2D3748' },
  varianceBox: { background:'#FAFBFC', borderRadius:10, padding:16, border:'1px solid #E0E4EA' },
  varianceRow: { display:'flex', gap:0, alignItems:'center' },
  varLabel: { fontSize:10, color:'#8896A9', fontWeight:600, marginBottom:4 },
  varVal: { fontSize:18, fontWeight:800, color:'#2D3748' },
  varDivider: { width:1, height:40, background:'#E0E4EA', margin:'0 20px' },
  reasonRow: { marginTop:10, paddingTop:10, borderTop:'1px solid #E0E4EA', display:'flex', gap:8, alignItems:'center' },
  reasonLabel: { fontSize:11, color:'#8896A9', fontWeight:600 },
  reasonVal: { fontSize:11, color:'#2D3748', fontStyle:'italic' },
  sectionTitle: { fontSize:11, fontWeight:800, color:'#8896A9', letterSpacing:1.5, marginBottom:10 },
  matrixTable: { width:'100%', borderCollapse:'collapse' },
  matrixTh: { padding:'8px 10px', fontSize:10, fontWeight:700, color:'#8896A9', letterSpacing:0.8, textAlign:'left', borderBottom:'2px solid #F0F2F5', background:'#FAFBFC', whiteSpace:'nowrap' },
  matrixTd: { padding:'9px 10px', fontSize:11, color:'#3D4A5C', borderBottom:'1px solid #F5F6FA', whiteSpace:'nowrap' },
  lifecycle: { display:'flex', alignItems:'center', gap:0, padding:'10px 0' },
  lcStep: { display:'flex', flexDirection:'column', alignItems:'center', gap:4 },
  lcDot: { width:26, height:26, borderRadius:13, display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700 },
  lcLabel: { fontSize:10, fontWeight:600, textAlign:'center', maxWidth:70, lineHeight:1.2 },
  lcLine: { flex:1, height:2, minWidth:20, maxWidth:50 },
  grid3: { display:'grid', gridTemplateColumns:'1fr 1fr 1fr', gap:12, marginBottom:16 },
  makerCheckerBadge: { padding:'8px 16px', background:'#EEF4FF', color:'#0050B3', border:'1px solid #C2D8FF', borderRadius:8, fontSize:12, fontWeight:700 },
};

Object.assign(window, { OpportunitiesPage, QuotesPage, oqStyles });
