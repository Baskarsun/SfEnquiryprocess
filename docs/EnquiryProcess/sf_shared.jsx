// Sundaram Finance — Shared Theme & UI Components
const SF = {
  navy: '#1B3A6B',
  navyDark: '#0F2344',
  navyLight: '#2A4F8F',
  gold: '#C8992B',
  goldLight: '#EDD69C',
  goldBg: '#FBF6EC',
  bg: '#EEF2FA',
  white: '#FFFFFF',
  success: '#0D7A4A',
  successBg: '#E6F6EF',
  error: '#C0392B',
  errorBg: '#FDECEA',
  warning: '#D97706',
  warningBg: '#FFFBEB',
  info: '#2563EB',
  infoBg: '#EFF6FF',
  text: '#111827',
  muted: '#6B7280',
  border: '#DDE3F0',
};

const STEPS = [
  { id: 'login',      label: 'Authentication',   icon: '🔐', desc: 'User & device verification' },
  { id: 'contract',   label: 'Contract Details',  icon: '📋', desc: 'Asset & loan information' },
  { id: 'applicants', label: 'Applicants',         icon: '👥', desc: 'Main & co-applicants' },
  { id: 'kyc',        label: 'KYC Documents',     icon: '📄', desc: 'Upload identity proofs' },
  { id: 'review',     label: 'Review',             icon: '🔍', desc: 'Verify all information' },
  { id: 'processing', label: 'Processing',         icon: '⚡', desc: 'Validation & submission' },
  { id: 'complete',   label: 'Complete',           icon: '✅', desc: 'Enquiry confirmed' },
];

function SFLogo({ white = false, compact = false }) {
  const c = white ? '#FFF' : SF.navy;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div style={{
        width: compact ? 32 : 40, height: compact ? 32 : 40,
        background: `linear-gradient(135deg, ${SF.gold} 0%, #B8871E 100%)`,
        borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 2px 8px rgba(200,153,43,0.35)', flexShrink: 0,
      }}>
        <span style={{ color: '#FFF', fontWeight: 800, fontSize: compact ? 13 : 16, fontFamily: 'inherit' }}>SF</span>
      </div>
      {!compact && (
        <div>
          <div style={{ color: c, fontWeight: 800, fontSize: 14, letterSpacing: '0.05em', lineHeight: 1.1 }}>SUNDARAM</div>
          <div style={{ color: white ? 'rgba(255,255,255,0.55)' : SF.muted, fontWeight: 500, fontSize: 10, letterSpacing: '0.12em' }}>FINANCE LIMITED</div>
        </div>
      )}
    </div>
  );
}

function SFSidebar({ step, user, isMobile }) {
  if (isMobile) {
    return (
      <div style={{
        background: `linear-gradient(90deg, ${SF.navyDark} 0%, ${SF.navy} 100%)`,
        padding: '12px 16px', display: 'flex', alignItems: 'center',
        justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 100,
        boxShadow: '0 2px 16px rgba(0,0,0,0.2)',
      }}>
        <SFLogo white compact />
        <div style={{ display: 'flex', gap: 5 }}>
          {STEPS.map((s, i) => (
            <div key={s.id} style={{
              width: i < 6 ? 7 : 0, height: 7, borderRadius: '50%',
              background: i < step ? SF.gold : i === step ? '#FFF' : 'rgba(255,255,255,0.25)',
              transition: 'all 0.3s', flexShrink: 0,
              display: i < 6 ? 'block' : 'none',
            }}/>
          ))}
        </div>
        <div style={{ color: 'rgba(255,255,255,0.7)', fontSize: 11, fontWeight: 600 }}>
          {user?.userId || '—'}
        </div>
      </div>
    );
  }
  return (
    <div style={{
      width: 268, minWidth: 268,
      background: `linear-gradient(180deg, ${SF.navyDark} 0%, ${SF.navy} 60%, ${SF.navyLight} 100%)`,
      display: 'flex', flexDirection: 'column',
      height: '100vh', position: 'sticky', top: 0, overflowY: 'auto',
    }}>
      <div style={{ padding: '28px 24px 22px', borderBottom: '1px solid rgba(255,255,255,0.09)' }}>
        <SFLogo white />
        <div style={{ marginTop: 14, padding: '8px 12px', background: 'rgba(255,255,255,0.06)', borderRadius: 8 }}>
          <div style={{ color: 'rgba(255,255,255,0.4)', fontSize: 10, letterSpacing: '0.1em' }}>PORTAL</div>
          <div style={{ color: 'rgba(255,255,255,0.85)', fontSize: 12, fontWeight: 600, marginTop: 2 }}>Loan Enquiry Generation</div>
        </div>
      </div>
      <div style={{ flex: 1, padding: '20px 14px', display: 'flex', flexDirection: 'column', gap: 3 }}>
        {STEPS.map((s, i) => {
          const done = i < step;
          const active = i === step;
          return (
            <div key={s.id} style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '10px 12px', borderRadius: 10,
              background: active ? 'rgba(200,153,43,0.13)' : 'transparent',
              border: active ? '1px solid rgba(200,153,43,0.28)' : '1px solid transparent',
              opacity: !done && !active ? 0.38 : 1,
              transition: 'all 0.25s',
            }}>
              <div style={{
                width: 34, height: 34, borderRadius: '50%', flexShrink: 0,
                background: done ? `linear-gradient(135deg, ${SF.gold}, #B8871E)` : active ? 'rgba(200,153,43,0.22)' : 'rgba(255,255,255,0.07)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: done ? 15 : 16,
                border: active ? `2px solid ${SF.gold}` : done ? 'none' : '1.5px solid rgba(255,255,255,0.12)',
                boxShadow: done ? '0 2px 8px rgba(200,153,43,0.3)' : 'none',
              }}>
                {done ? <span style={{ color: '#FFF', fontWeight: 700, fontSize: 14 }}>✓</span> : s.icon}
              </div>
              <div>
                <div style={{ fontSize: 13, fontWeight: active ? 600 : 400, color: active ? SF.gold : done ? 'rgba(255,255,255,0.88)' : 'rgba(255,255,255,0.55)' }}>
                  {s.label}
                </div>
                {active && <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.38)', marginTop: 1 }}>{s.desc}</div>}
              </div>
            </div>
          );
        })}
      </div>
      {user?.userId && (
        <div style={{ margin: '0 14px 20px', padding: '12px 14px', background: 'rgba(255,255,255,0.06)', borderRadius: 10, border: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ color: 'rgba(255,255,255,0.35)', fontSize: 10, letterSpacing: '0.1em' }}>AUTHENTICATED AS</div>
          <div style={{ color: SF.gold, fontSize: 13, fontWeight: 700, marginTop: 3 }}>{user.userId}</div>
          <div style={{ color: 'rgba(255,255,255,0.4)', fontSize: 11 }}>Field Officer</div>
        </div>
      )}
    </div>
  );
}

function SFInput({ label, value, onChange, error, required, type = 'text', placeholder, helper, readOnly, prefix, suffix, maxLength }) {
  const [focused, setFocused] = React.useState(false);
  const [touched, setTouched] = React.useState(false);
  // Show error if parent passed one (parent gates via fErr) OR user has touched this field
  const showError = !!error;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
      {label && (
        <label style={{ fontSize: 11, fontWeight: 700, color: SF.muted, letterSpacing: '0.07em', textTransform: 'uppercase' }}>
          {label}{required && <span style={{ color: SF.error, marginLeft: 2 }}>*</span>}
        </label>
      )}
      <div style={{ position: 'relative' }}>
        {prefix && <span style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: SF.muted, fontSize: 13, pointerEvents: 'none' }}>{prefix}</span>}
        <input
          type={type} value={value}
          onChange={e => onChange(e.target.value)}
          onFocus={() => setFocused(true)}
          onBlur={() => { setFocused(false); setTouched(true); }}
          placeholder={placeholder} readOnly={readOnly}
          maxLength={maxLength}
          style={{
            width: '100%', boxSizing: 'border-box',
            padding: `10px ${suffix ? '40px' : '13px'} 10px ${prefix ? '32px' : '13px'}`,
            border: `1.5px solid ${showError ? SF.error : focused ? SF.navy : SF.border}`,
            borderRadius: 8, fontSize: 14, color: SF.text,
            background: readOnly ? '#F5F8FF' : SF.white,
            outline: 'none', transition: 'border 0.15s',
            fontFamily: 'inherit',
          }}
        />
        {suffix && <span style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: SF.muted, fontSize: 12, pointerEvents: 'none' }}>{suffix}</span>}
      </div>
      {showError && <div style={{ fontSize: 12, color: SF.error, display: 'flex', alignItems: 'center', gap: 4 }}><span>⚠</span>{error}</div>}
      {helper && !showError && <div style={{ fontSize: 12, color: SF.muted }}>{helper}</div>}
    </div>
  );
}

function SFSelect({ label, value, onChange, options, required, error, placeholder }) {
  const [touched, setTouched] = React.useState(false);
  const showError = !!error;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
      {label && (
        <label style={{ fontSize: 11, fontWeight: 700, color: SF.muted, letterSpacing: '0.07em', textTransform: 'uppercase' }}>
          {label}{required && <span style={{ color: SF.error, marginLeft: 2 }}>*</span>}
        </label>
      )}
      <div style={{ position: 'relative' }}>
        <select
          value={value} onChange={e => onChange(e.target.value)} onBlur={() => setTouched(true)}
          style={{
            width: '100%', appearance: 'none', padding: '10px 36px 10px 13px',
            border: `1.5px solid ${showError ? SF.error : SF.border}`, borderRadius: 8,
            fontSize: 14, color: value ? SF.text : SF.muted, background: SF.white,
            outline: 'none', cursor: 'pointer', fontFamily: 'inherit',
          }}
        >
          {placeholder && <option value="">{placeholder}</option>}
          {options.map(o => <option key={typeof o === 'string' ? o : o.value} value={typeof o === 'string' ? o : o.value}>{typeof o === 'string' ? o : o.label}</option>)}
        </select>
        <span style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none', color: SF.muted, fontSize: 11 }}>▼</span>
      </div>
      {showError && <div style={{ fontSize: 12, color: SF.error }}><span>⚠ </span>{error}</div>}
    </div>
  );
}

function SFButton({ children, onClick, variant = 'primary', disabled, loading, fullWidth, size = 'md' }) {
  const pad = { sm: '8px 16px', md: '11px 24px', lg: '14px 32px' };
  const fs = { sm: 13, md: 14, lg: 15 };
  const base = {
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 7,
    padding: pad[size], fontSize: fs[size], fontWeight: 700, borderRadius: 9,
    border: 'none', cursor: disabled || loading ? 'not-allowed' : 'pointer',
    transition: 'all 0.15s', fontFamily: 'inherit', width: fullWidth ? '100%' : 'auto',
    opacity: disabled ? 0.55 : 1, letterSpacing: '0.01em',
  };
  const vMap = {
    primary: { background: SF.navy, color: '#FFF', boxShadow: '0 2px 10px rgba(27,58,107,0.25)' },
    gold: { background: `linear-gradient(135deg, ${SF.gold}, #B8871E)`, color: '#FFF', boxShadow: '0 2px 10px rgba(200,153,43,0.3)' },
    outline: { background: 'transparent', color: SF.navy, border: `1.5px solid ${SF.navy}` },
    danger: { background: SF.error, color: '#FFF' },
    ghost: { background: 'transparent', color: SF.muted, border: `1.5px solid ${SF.border}` },
  };
  return (
    <button onClick={onClick} disabled={disabled || loading} style={{ ...base, ...vMap[variant] }}>
      {loading ? <span style={{ display: 'inline-block', animation: 'spin 0.8s linear infinite' }}>⟳</span> : children}
    </button>
  );
}

function SFCard({ children, title, action, noPad }) {
  return (
    <div style={{ background: SF.white, borderRadius: 14, boxShadow: '0 2px 14px rgba(27,58,107,0.08)', overflow: 'hidden' }}>
      {title && (
        <div style={{ padding: '14px 20px', borderBottom: `1px solid ${SF.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#FAFCFF' }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: SF.navy, display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ width: 3, height: 16, background: SF.gold, borderRadius: 2 }} />
            {title}
          </div>
          {action}
        </div>
      )}
      <div style={noPad ? {} : { padding: 20 }}>{children}</div>
    </div>
  );
}

function SFAlert({ type = 'info', children }) {
  const cfg = {
    info: { bg: SF.infoBg, border: SF.info, icon: 'ℹ️' },
    success: { bg: SF.successBg, border: SF.success, icon: '✅' },
    error: { bg: SF.errorBg, border: SF.error, icon: '⚠️' },
    warning: { bg: SF.warningBg, border: SF.warning, icon: '⚠️' },
  };
  const c = cfg[type];
  return (
    <div style={{ background: c.bg, border: `1px solid ${c.border}`, borderRadius: 9, padding: '11px 15px', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
      <span style={{ fontSize: 15, flexShrink: 0 }}>{c.icon}</span>
      <div style={{ fontSize: 13, color: SF.text, lineHeight: 1.55 }}>{children}</div>
    </div>
  );
}

function FormGrid({ children, cols = 2 }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, gap: 16 }}>
      {children}
    </div>
  );
}

function SectionHead({ label, sub }) {
  return (
    <div style={{ marginBottom: 16, paddingBottom: 10, borderBottom: `1px solid ${SF.border}` }}>
      <div style={{ fontSize: 12, fontWeight: 700, color: SF.navy, textTransform: 'uppercase', letterSpacing: '0.08em', display: 'flex', alignItems: 'center', gap: 7 }}>
        <span style={{ display: 'inline-block', width: 3, height: 13, background: SF.gold, borderRadius: 2 }} />
        {label}
      </div>
      {sub && <div style={{ fontSize: 12, color: SF.muted, marginTop: 3, paddingLeft: 10 }}>{sub}</div>}
    </div>
  );
}

function SFBadge({ children, color }) {
  const col = color || SF.navy;
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', padding: '3px 10px', borderRadius: 20, fontSize: 11, fontWeight: 600, background: col + '18', color: col, border: `1px solid ${col}28` }}>
      {children}
    </span>
  );
}

function NavButtons({ onBack, onNext, backLabel = '← Back', nextLabel = 'Continue →', nextVariant = 'primary', disabled, loading }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 8 }}>
      <SFButton onClick={onBack} variant="ghost" size="md">{backLabel}</SFButton>
      <SFButton onClick={onNext} variant={nextVariant} size="md" disabled={disabled} loading={loading}>{nextLabel}</SFButton>
    </div>
  );
}

Object.assign(window, {
  SF, STEPS,
  SFLogo, SFSidebar,
  SFInput, SFSelect, SFButton, SFCard, SFAlert,
  FormGrid, SectionHead, SFBadge, NavButtons,
});
