
// ─── App Shell: Sidebar + Top Nav ────────────────────────────────────────────

const NAV_STAGES = [
  { id: 'dashboard', label: 'Dashboard', icon: '◈', color: '#FFB81C' },
  { id: 'leads', label: 'Lead Generation', icon: '①', color: '#4DABF7', stage: 1 },
  { id: 'prospects', label: 'Enquiry / Prospect', icon: '②', color: '#69DB7C', stage: 2 },
  { id: 'opportunities', label: 'Opportunity', icon: '③', color: '#FFA94D', stage: 3 },
  { id: 'quotes', label: 'Pricing & Quote', icon: '④', color: '#DA77F2', stage: 4 },
  { id: 'applications', label: 'Application', icon: '⑤', color: '#F783AC', stage: 5 },
  { id: 'customers', label: 'Customer Creation', icon: '⑥', color: '#63E6BE', stage: 6 },
];

function AppShell({ page, setPage, role, setRole, children }) {
  const [roleOpen, setRoleOpen] = React.useState(false);
  const currentRole = window.SF_ROLES.find(r => r.id === role);

  return (
    <div style={shellStyles.root}>
      {/* ── Sidebar ── */}
      <aside style={shellStyles.sidebar}>
        <div style={shellStyles.logo}>
          <div style={shellStyles.logoMark}>SF</div>
          <div>
            <div style={shellStyles.logoTitle}>SUNDARAM</div>
            <div style={shellStyles.logoSub}>FINANCE · LEASING LOS</div>
          </div>
        </div>

        <div style={shellStyles.navSection}>
          <div style={shellStyles.navLabel}>ORIGINATION STAGES</div>
          {NAV_STAGES.map(s => {
            const active = page === s.id;
            return (
              <button key={s.id} style={{...shellStyles.navItem, ...(active ? shellStyles.navItemActive : {})}}
                onClick={() => setPage(s.id)}>
                <span style={{...shellStyles.navIcon, color: active ? '#fff' : s.color}}>{s.icon}</span>
                <span style={shellStyles.navText}>{s.label}</span>
                {active && <span style={shellStyles.navPip} />}
              </button>
            );
          })}
        </div>

        <div style={shellStyles.sidebarFooter}>
          <div style={shellStyles.stageFunnel}>
            <div style={shellStyles.funnelTitle}>PIPELINE TODAY</div>
            {[
              { l: 'Leads', v: window.SF_KPI.totalLeads, c: '#4DABF7' },
              { l: 'Prospects', v: window.SF_KPI.prospects, c: '#69DB7C' },
              { l: 'Opportunities', v: window.SF_KPI.opportunities, c: '#FFA94D' },
              { l: 'Quotes Locked', v: window.SF_KPI.quotesLocked, c: '#DA77F2' },
              { l: 'Applications', v: window.SF_KPI.applications, c: '#F783AC' },
              { l: 'Customers', v: window.SF_KPI.customers, c: '#63E6BE' },
            ].map((f, i) => (
              <div key={i} style={shellStyles.funnelRow}>
                <span style={{...shellStyles.funnelDot, background: f.c}} />
                <span style={shellStyles.funnelLabel}>{f.l}</span>
                <span style={{...shellStyles.funnelVal, color: f.c}}>{f.v}</span>
              </div>
            ))}
          </div>
        </div>
      </aside>

      {/* ── Main ── */}
      <div style={shellStyles.main}>
        {/* Top Bar */}
        <header style={shellStyles.topbar}>
          <div style={shellStyles.breadcrumb}>
            <span style={shellStyles.breadcrumbHome}>Leasing LOS</span>
            <span style={shellStyles.breadcrumbSep}>›</span>
            <span style={shellStyles.breadcrumbCurrent}>{NAV_STAGES.find(s=>s.id===page)?.label || 'Dashboard'}</span>
          </div>
          <div style={shellStyles.topActions}>
            {/* Alert badges */}
            <div style={shellStyles.alertBadge} title="AML Flagged">
              🛡️ <span style={shellStyles.alertNum}>{window.SF_KPI.amlFlagged}</span>
            </div>
            <div style={shellStyles.alertBadge} title="KYC Pending">
              📋 <span style={shellStyles.alertNum}>{window.SF_KPI.pendingKyc}</span>
            </div>
            {/* Role Switcher */}
            <div style={{position:'relative'}}>
              <button style={shellStyles.roleBtn} onClick={() => setRoleOpen(!roleOpen)}>
                <span style={shellStyles.roleIcon}>{currentRole?.icon}</span>
                <span style={shellStyles.roleLabel}>{currentRole?.label}</span>
                <span style={{marginLeft:6, opacity:0.6}}>▾</span>
              </button>
              {roleOpen && (
                <div style={shellStyles.roleDropdown}>
                  <div style={shellStyles.dropdownHeader}>Switch Role</div>
                  {window.SF_ROLES.map(r => (
                    <button key={r.id} style={{...shellStyles.dropdownItem, ...(r.id===role ? shellStyles.dropdownItemActive : {})}}
                      onClick={() => { setRole(r.id); setRoleOpen(false); }}>
                      <span>{r.icon}</span> {r.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div style={shellStyles.avatar}>RK</div>
          </div>
        </header>

        {/* Content */}
        <main style={shellStyles.content}>
          {children}
        </main>
      </div>
    </div>
  );
}

const shellStyles = {
  root: { display:'flex', height:'100vh', overflow:'hidden', fontFamily:"'Plus Jakarta Sans', sans-serif", background:'#F0F2F5' },
  sidebar: { width:230, background:'#002060', display:'flex', flexDirection:'column', flexShrink:0, overflow:'hidden' },
  logo: { display:'flex', alignItems:'center', gap:10, padding:'20px 16px 16px', borderBottom:'1px solid rgba(255,255,255,0.08)' },
  logoMark: { width:36, height:36, borderRadius:8, background:'#FFB81C', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:900, fontSize:14, color:'#002060', flexShrink:0 },
  logoTitle: { fontSize:12, fontWeight:800, color:'#fff', letterSpacing:2 },
  logoSub: { fontSize:8, color:'rgba(255,255,255,0.45)', letterSpacing:1.5, marginTop:1 },
  navSection: { flex:1, padding:'12px 8px', overflowY:'auto' },
  navLabel: { fontSize:9, color:'rgba(255,255,255,0.3)', letterSpacing:2, padding:'8px 8px 6px', fontWeight:700 },
  navItem: { display:'flex', alignItems:'center', gap:8, width:'100%', padding:'9px 10px', borderRadius:8, border:'none', background:'transparent', cursor:'pointer', textAlign:'left', transition:'all 0.15s', marginBottom:2 },
  navItemActive: { background:'rgba(255,184,28,0.18)', borderLeft:'3px solid #FFB81C' },
  navIcon: { fontSize:15, width:20, textAlign:'center', flexShrink:0 },
  navText: { fontSize:12, color:'rgba(255,255,255,0.82)', fontWeight:500 },
  navPip: { marginLeft:'auto', width:6, height:6, borderRadius:3, background:'#FFB81C' },
  sidebarFooter: { padding:'12px 12px 16px' },
  stageFunnel: { background:'rgba(255,255,255,0.05)', borderRadius:10, padding:12 },
  funnelTitle: { fontSize:9, color:'rgba(255,255,255,0.3)', letterSpacing:2, marginBottom:8, fontWeight:700 },
  funnelRow: { display:'flex', alignItems:'center', gap:7, marginBottom:5 },
  funnelDot: { width:6, height:6, borderRadius:3, flexShrink:0 },
  funnelLabel: { fontSize:10, color:'rgba(255,255,255,0.5)', flex:1 },
  funnelVal: { fontSize:11, fontWeight:700 },
  main: { flex:1, display:'flex', flexDirection:'column', overflow:'hidden' },
  topbar: { height:56, background:'#fff', borderBottom:'1px solid #E8ECF0', display:'flex', alignItems:'center', justifyContent:'space-between', padding:'0 24px', flexShrink:0, boxShadow:'0 1px 4px rgba(0,32,96,0.06)' },
  breadcrumb: { display:'flex', alignItems:'center', gap:8 },
  breadcrumbHome: { fontSize:12, color:'#8896A9', fontWeight:500 },
  breadcrumbSep: { color:'#C4CDD8', fontSize:14 },
  breadcrumbCurrent: { fontSize:13, color:'#002060', fontWeight:700 },
  topActions: { display:'flex', alignItems:'center', gap:12 },
  alertBadge: { display:'flex', alignItems:'center', gap:4, padding:'4px 10px', background:'#F0F2F5', borderRadius:20, fontSize:11, color:'#555', cursor:'pointer', border:'1px solid #E0E4EA' },
  alertNum: { fontWeight:700, color:'#D63B3B' },
  roleBtn: { display:'flex', alignItems:'center', gap:6, padding:'6px 14px', background:'#002060', color:'#fff', border:'none', borderRadius:20, cursor:'pointer', fontSize:12, fontWeight:600 },
  roleIcon: { fontSize:13 },
  roleLabel: { fontSize:12 },
  roleDropdown: { position:'absolute', right:0, top:40, background:'#fff', borderRadius:12, boxShadow:'0 8px 32px rgba(0,32,96,0.15)', border:'1px solid #E0E4EA', minWidth:200, zIndex:1000, overflow:'hidden' },
  dropdownHeader: { padding:'10px 16px', fontSize:10, fontWeight:700, letterSpacing:1.5, color:'#8896A9', borderBottom:'1px solid #F0F2F5' },
  dropdownItem: { display:'flex', alignItems:'center', gap:8, width:'100%', padding:'9px 16px', border:'none', background:'transparent', cursor:'pointer', fontSize:12, color:'#2D3748', textAlign:'left' },
  dropdownItemActive: { background:'#EEF2FF', color:'#002060', fontWeight:700 },
  avatar: { width:32, height:32, borderRadius:16, background:'#FFB81C', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:800, color:'#002060' },
  content: { flex:1, overflowY:'auto', padding:24 },
};

Object.assign(window, { AppShell, NAV_STAGES });
