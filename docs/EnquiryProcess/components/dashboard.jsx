
// ─── Dashboard ────────────────────────────────────────────────────────────────

function Dashboard({ setPage, role }) {
  const K = window.SF_KPI;

  const kpis = [
    { label: 'Total Leads', value: K.totalLeads, sub: '↑12 this week', color: '#4DABF7', icon: '⟡', page: 'leads' },
    { label: 'Active Prospects', value: K.prospects, sub: '↑8 this week', color: '#69DB7C', icon: '◎', page: 'prospects' },
    { label: 'Opportunities', value: K.opportunities, sub: '↑5 this week', color: '#FFA94D', icon: '◈', page: 'opportunities' },
    { label: 'Quotes Locked', value: K.quotesLocked, sub: '↑3 this week', color: '#DA77F2', icon: '◇', page: 'quotes' },
    { label: 'Applications', value: K.applications, sub: '2 sanctioned', color: '#F783AC', icon: '◆', page: 'applications' },
    { label: 'Customers Created', value: K.customers, sub: 'this month', color: '#63E6BE', icon: '✦', page: 'customers' },
  ];

  const alerts = [
    { type: 'AML', label: 'AML Flagged — Tata Motors Finance', severity: 'high', time: '2h ago', page: 'prospects' },
    { type: 'KYC', label: '12 Prospects with KYC Pending > 3 days', severity: 'medium', time: '5h ago', page: 'prospects' },
    { type: 'QUOTE', label: 'Quote Q-202600012 awaiting Manager approval', severity: 'medium', time: '1d ago', page: 'quotes' },
    { type: 'SLA', label: '3 Leads uncontacted > SLA threshold', severity: 'low', time: '1d ago', page: 'leads' },
  ];

  const funnelData = [
    { stage: 'Leads', count: K.totalLeads, pct: 100, color: '#4DABF7' },
    { stage: 'Prospects', count: K.prospects, pct: Math.round(K.prospects/K.totalLeads*100), color: '#69DB7C' },
    { stage: 'Opportunities', count: K.opportunities, pct: Math.round(K.opportunities/K.totalLeads*100), color: '#FFA94D' },
    { stage: 'Quotes', count: K.quotesLocked, pct: Math.round(K.quotesLocked/K.totalLeads*100), color: '#DA77F2' },
    { stage: 'Applications', count: K.applications, pct: Math.round(K.applications/K.totalLeads*100), color: '#F783AC' },
    { stage: 'Customers', count: K.customers, pct: Math.round(K.customers/K.totalLeads*100), color: '#63E6BE' },
  ];

  const recentLeads = window.SF_LEADS.slice(0,5);
  const sevColor = { high:'#FF4D4D', medium:'#FFB81C', low:'#69DB7C' };

  return (
    <div>
      {/* Header */}
      <div style={dbStyles.header}>
        <div>
          <div style={dbStyles.greeting}>Good Morning, Rajesh Kumar</div>
          <div style={dbStyles.date}>Thursday, 24 April 2026 · Chennai Main Branch</div>
        </div>
        <button style={dbStyles.newLeadBtn} onClick={() => setPage('leads')}>
          + New Lead
        </button>
      </div>

      {/* KPI Cards */}
      <div style={dbStyles.kpiGrid}>
        {kpis.map((k, i) => (
          <div key={i} style={{...dbStyles.kpiCard, borderTop:`3px solid ${k.color}`}} onClick={() => setPage(k.page)}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start'}}>
              <div>
                <div style={dbStyles.kpiLabel}>{k.label}</div>
                <div style={{...dbStyles.kpiValue, color: k.color}}>{k.value}</div>
                <div style={dbStyles.kpiSub}>{k.sub}</div>
              </div>
              <span style={{...dbStyles.kpiIcon, color: k.color}}>{k.icon}</span>
            </div>
          </div>
        ))}
      </div>

      <div style={dbStyles.row2}>
        {/* Funnel */}
        <div style={dbStyles.card}>
          <div style={dbStyles.cardHeader}>
            <span style={dbStyles.cardTitle}>Origination Funnel</span>
            <span style={dbStyles.cardMeta}>Last 30 days</span>
          </div>
          <div style={{padding:'4px 0'}}>
            {funnelData.map((f, i) => (
              <div key={i} style={dbStyles.funnelRow}>
                <div style={dbStyles.funnelStage}>{f.stage}</div>
                <div style={dbStyles.funnelBarWrap}>
                  <div style={{...dbStyles.funnelBar, width:`${f.pct}%`, background: f.color}} />
                </div>
                <div style={{...dbStyles.funnelCount, color: f.color}}>{f.count}</div>
                <div style={dbStyles.funnelPct}>{f.pct}%</div>
              </div>
            ))}
          </div>
          <div style={dbStyles.convRate}>
            Overall Conversion Rate: <strong style={{color:'#FFB81C'}}>{K.conversionRate}%</strong>
            &nbsp;·&nbsp;Avg TAT: <strong style={{color:'#69DB7C'}}>{K.avgTat} days</strong>
          </div>
        </div>

        {/* Alerts */}
        <div style={dbStyles.card}>
          <div style={dbStyles.cardHeader}>
            <span style={dbStyles.cardTitle}>Action Required</span>
            <span style={{...dbStyles.badge, background:'#FF4D4D22', color:'#FF4D4D'}}>{alerts.length}</span>
          </div>
          {alerts.map((a, i) => (
            <div key={i} style={dbStyles.alertRow} onClick={() => setPage(a.page)}>
              <div style={{...dbStyles.alertDot, background: sevColor[a.severity]}} />
              <div style={{flex:1}}>
                <div style={dbStyles.alertText}>{a.label}</div>
                <div style={dbStyles.alertTime}>{a.time}</div>
              </div>
              <span style={{...dbStyles.alertTag, background: sevColor[a.severity]+'22', color: sevColor[a.severity]}}>{a.type}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Leads */}
      <div style={dbStyles.card}>
        <div style={dbStyles.cardHeader}>
          <span style={dbStyles.cardTitle}>Recent Leads</span>
          <button style={dbStyles.viewAllBtn} onClick={() => setPage('leads')}>View All →</button>
        </div>
        <table style={dbStyles.table}>
          <thead>
            <tr>{['Lead ID','Name','Type','Asset','Status','Assignee','Dedup',''].map((h,i)=>
              <th key={i} style={dbStyles.th}>{h}</th>
            )}</tr>
          </thead>
          <tbody>
            {recentLeads.map((l, i) => (
              <tr key={i} style={i%2===0?{}:{background:'#F8FAFC'}}>
                <td style={dbStyles.td}><span style={dbStyles.idBadge}>{l.id}</span></td>
                <td style={dbStyles.td}><strong style={{color:'#002060'}}>{l.name}</strong></td>
                <td style={dbStyles.td}><span style={{...dbStyles.typeBadge, background: l.type==='Individual'?'#EEF6FF':'#FFF4E6', color: l.type==='Individual'?'#0050B3':'#E67700'}}>{l.type}</span></td>
                <td style={dbStyles.td}>{l.asset}</td>
                <td style={dbStyles.td}><StatusChip status={l.status} /></td>
                <td style={dbStyles.td}>{l.assignee}</td>
                <td style={dbStyles.td}><DedupChip label={l.dedup} /></td>
                <td style={dbStyles.td}><button style={dbStyles.actionBtn} onClick={() => setPage('leads')}>View →</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatusChip({ status }) {
  const map = { Hot:['#FF4D4D','#FFF0F0'], Warm:['#FFB81C','#FFF9E6'], Cold:['#74C0FC','#EEF6FF'],
    Active:['#69DB7C','#F0FFF4'], Flagged:['#FF4D4D','#FFF0F0'], Clear:['#69DB7C','#F0FFF4'],
    Locked:['#DA77F2','#F8F0FF'], Draft:['#ADB5BD','#F8F9FA'], Pending:['#FFB81C','#FFF9E6'],
    Approved:['#69DB7C','#F0FFF4'], Quoted:['#4DABF7','#EEF6FF'], Open:['#FFA94D','#FFF4E6'],
    Submitted:['#4DABF7','#EEF6FF'], Complete:['#69DB7C','#F0FFF4'], 'In Progress':['#FFA94D','#FFF4E6'],
    Underwriting:['#DA77F2','#F8F0FF'], Negotiation:['#FFA94D','#FFF4E6'],
    Qualified:['#69DB7C','#F0FFF4'], 'In Review':['#4DABF7','#EEF6FF'], New:['#ADB5BD','#F8F9FA'],
    QuoteLocked:['#DA77F2','#F8F0FF']
  };
  const [color, bg] = map[status] || ['#555','#eee'];
  return <span style={{padding:'2px 9px', borderRadius:20, fontSize:11, fontWeight:700, color, background:bg, whiteSpace:'nowrap'}}>{status}</span>;
}
function DedupChip({ label }) {
  const map = { New:['#69DB7C','#F0FFF4'], PossibleExisting:['#FFB81C','#FFF9E6'], Unknown:['#ADB5BD','#F8F9FA'], Conflict:['#FF4D4D','#FFF0F0'] };
  const [color, bg] = map[label] || ['#555','#eee'];
  return <span style={{padding:'2px 9px', borderRadius:20, fontSize:11, fontWeight:700, color, background:bg}}>{label}</span>;
}

const dbStyles = {
  header: { display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:24 },
  greeting: { fontSize:22, fontWeight:800, color:'#002060', letterSpacing:-0.5 },
  date: { fontSize:12, color:'#8896A9', marginTop:2 },
  newLeadBtn: { padding:'10px 22px', background:'#FFB81C', color:'#002060', border:'none', borderRadius:10, fontWeight:800, fontSize:13, cursor:'pointer' },
  kpiGrid: { display:'grid', gridTemplateColumns:'repeat(6,1fr)', gap:12, marginBottom:20 },
  kpiCard: { background:'#fff', borderRadius:12, padding:16, cursor:'pointer', transition:'transform 0.1s', boxShadow:'0 1px 4px rgba(0,32,96,0.07)' },
  kpiLabel: { fontSize:11, color:'#8896A9', fontWeight:600, marginBottom:6 },
  kpiValue: { fontSize:32, fontWeight:900, lineHeight:1 },
  kpiSub: { fontSize:10, color:'#B2BECC', marginTop:4 },
  kpiIcon: { fontSize:22, opacity:0.35 },
  row2: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:20 },
  card: { background:'#fff', borderRadius:14, padding:20, boxShadow:'0 1px 4px rgba(0,32,96,0.07)', marginBottom:16 },
  cardHeader: { display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16 },
  cardTitle: { fontSize:14, fontWeight:800, color:'#002060' },
  cardMeta: { fontSize:11, color:'#8896A9' },
  badge: { padding:'2px 10px', borderRadius:20, fontSize:11, fontWeight:700 },
  funnelRow: { display:'flex', alignItems:'center', gap:10, marginBottom:10 },
  funnelStage: { width:90, fontSize:11, color:'#5A6A7E', fontWeight:600 },
  funnelBarWrap: { flex:1, height:10, background:'#F0F2F5', borderRadius:5, overflow:'hidden' },
  funnelBar: { height:'100%', borderRadius:5, transition:'width 0.4s' },
  funnelCount: { width:30, fontSize:13, fontWeight:800, textAlign:'right' },
  funnelPct: { width:36, fontSize:10, color:'#8896A9', textAlign:'right' },
  convRate: { marginTop:12, fontSize:12, color:'#8896A9', borderTop:'1px solid #F0F2F5', paddingTop:10 },
  alertRow: { display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderBottom:'1px solid #F5F6FA', cursor:'pointer' },
  alertDot: { width:8, height:8, borderRadius:4, flexShrink:0 },
  alertText: { fontSize:12, color:'#2D3748', fontWeight:500 },
  alertTime: { fontSize:10, color:'#B2BECC', marginTop:2 },
  alertTag: { padding:'2px 8px', borderRadius:6, fontSize:10, fontWeight:700, flexShrink:0 },
  table: { width:'100%', borderCollapse:'collapse' },
  th: { padding:'8px 10px', fontSize:10, fontWeight:700, color:'#8896A9', letterSpacing:0.8, textAlign:'left', borderBottom:'2px solid #F0F2F5', background:'#FAFBFC' },
  td: { padding:'10px 10px', fontSize:12, color:'#3D4A5C', borderBottom:'1px solid #F5F6FA' },
  idBadge: { fontFamily:'monospace', fontSize:11, color:'#0050B3', background:'#EEF4FF', padding:'2px 6px', borderRadius:4 },
  typeBadge: { padding:'2px 8px', borderRadius:4, fontSize:11, fontWeight:600 },
  actionBtn: { padding:'3px 10px', background:'#EEF4FF', color:'#0050B3', border:'none', borderRadius:6, fontSize:11, fontWeight:700, cursor:'pointer' },
  viewAllBtn: { fontSize:12, color:'#0050B3', fontWeight:700, background:'transparent', border:'none', cursor:'pointer' },
};

Object.assign(window, { Dashboard, StatusChip, DedupChip, dbStyles });
