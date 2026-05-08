// Main App — state management & screen routing

const INITIAL_FORM = () => ({
  user: { userId: '', imei: '', uuid: '', appVersion: '' },
  contract: {
    enquiryDate: new Date().toISOString().split('T')[0],
    contractType: '', assetClass: '', loanPurpose: '',
    assetMake: '', assetModel: '', assetType: '', fuelType: '', assetUsage: '', numUnits: '1',
    assetCost: '', financeAmount: '', loanTenure: '', interestRate: '', netIncome: '', repaymentFreq: '',
    dealerCode: '', businessSource: '', appraisalCategory: '', assessmentCriteria: '', marketingEmployee: '', lmsLeadId: '', remarks: '',
  },
  applicants: [{ ...EMPTY_APPLICANT(), type: 'MAIN APPLICANT' }],
  kyc: { files: [] },
  modifyEnquiryNumber: null,
});

function useWindowWidth() {
  const [w, setW] = React.useState(window.innerWidth);
  React.useEffect(() => {
    const fn = () => setW(window.innerWidth);
    window.addEventListener('resize', fn);
    return () => window.removeEventListener('resize', fn);
  }, []);
  return w;
}

function App() {
  const [step, setStep] = React.useState(() => {
    try { return Number(localStorage.getItem('sf_enquiry_step') || 0); } catch { return 0; }
  });
  const [formData, setFormData] = React.useState(() => {
    try {
      const saved = localStorage.getItem('sf_enquiry_form');
      return saved ? { ...INITIAL_FORM(), ...JSON.parse(saved) } : INITIAL_FORM();
    } catch { return INITIAL_FORM(); }
  });
  const [result, setResult] = React.useState(null);
  const [error, setError] = React.useState(null);
  const [showTweaks, setShowTweaks] = React.useState(false);
  const [tweaks, setTweaks] = React.useState(/*EDITMODE-BEGIN*/{
    "primaryColor": "#1B3A6B",
    "accentColor": "#C8992B",
    "fontFamily": "Plus Jakarta Sans",
    "compactMode": false
  }/*EDITMODE-END*/);

  const width = useWindowWidth();
  const isMobile = width < 768;

  // Persist step & form
  React.useEffect(() => {
    try { localStorage.setItem('sf_enquiry_step', String(step)); } catch {}
  }, [step]);
  React.useEffect(() => {
    try { localStorage.setItem('sf_enquiry_form', JSON.stringify(formData)); } catch {}
  }, [formData]);

  // Tweaks bridge
  React.useEffect(() => {
    const handler = (e) => {
      if (e.data?.type === '__activate_edit_mode') setShowTweaks(true);
      if (e.data?.type === '__deactivate_edit_mode') setShowTweaks(false);
    };
    window.addEventListener('message', handler);
    window.parent.postMessage({ type: '__edit_mode_available' }, '*');
    return () => window.removeEventListener('message', handler);
  }, []);

  const applyTweak = (key, val) => {
    setTweaks(p => {
      const next = { ...p, [key]: val };
      window.parent.postMessage({ type: '__edit_mode_set_keys', edits: next }, '*');
      return next;
    });
  };

  const goTo = (s) => { setStep(s); window.scrollTo({ top: 0, behavior: 'smooth' }); };
  const reset = () => {
    setFormData(INITIAL_FORM());
    setResult(null); setError(null);
    localStorage.removeItem('sf_enquiry_step');
    localStorage.removeItem('sf_enquiry_form');
    goTo(0);
  };

  const contentStyle = {
    flex: 1, overflowY: 'auto', padding: isMobile ? '20px 16px' : '32px 40px',
    maxWidth: 780, width: '100%', margin: '0 auto',
    fontFamily: tweaks.fontFamily + ', sans-serif',
  };

  const screenProps = { formData, setFormData, onBack: () => goTo(step - 1), onNext: () => goTo(step + 1) };

  const renderScreen = () => {
    switch (step) {
      case 0: return <LoginScreen {...screenProps} onNext={() => goTo(1)} />;
      case 1: return <ContractForm {...screenProps} onBack={() => goTo(0)} onNext={() => goTo(2)} />;
      case 2: return <ApplicantForm {...screenProps} onBack={() => goTo(1)} onNext={() => goTo(3)} />;
      case 3: return <KYCUpload {...screenProps} onBack={() => goTo(2)} onNext={() => goTo(4)} />;
      case 4: return <ReviewScreen {...screenProps} onBack={() => goTo(3)} onNext={() => goTo(5)} onEditStep={s => goTo(s)} />;
      case 5: return (
        <ProcessingScreen
          formData={formData}
          onDone={(res) => { setResult(res); goTo(6); }}
        />
      );
      case 6:
        if (error) return <ErrorScreen error={error} onRetry={() => { setError(null); goTo(5); }} onBack={() => goTo(4)} />;
        return <SuccessScreen result={result || { enquiryNumber: 'SFL/25/0001/12345' }} formData={formData} onNewEnquiry={reset} onModify={() => { setFormData(p => ({ ...p, modifyEnquiryNumber: result?.enquiryNumber })); goTo(1); }} />;
      default: return null;
    }
  };

  // For login screen, full-bleed
  if (step === 0) {
    return (
      <>
        <style>{`
          @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
          * { box-sizing: border-box; margin: 0; padding: 0; }
          body { font-family: 'Plus Jakarta Sans', sans-serif; background: ${SF.bg}; }
          @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
          @keyframes bounceIn { 0% { transform: scale(0.3); opacity: 0; } 60% { transform: scale(1.1); } 100% { transform: scale(1); opacity: 1; } }
          select { appearance: none; }
          ::-webkit-scrollbar { width: 6px; } ::-webkit-scrollbar-track { background: transparent; } ::-webkit-scrollbar-thumb { background: ${SF.border}; border-radius: 3px; }
        `}</style>
        <div style={{ fontFamily: tweaks.fontFamily + ', sans-serif' }}>
          <LoginScreen formData={formData} setFormData={setFormData} onNext={() => goTo(1)} onBack={() => {}} />
        </div>
      </>
    );
  }

  return (
    <>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Plus Jakarta Sans', sans-serif; background: ${SF.bg}; }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
        @keyframes bounceIn { 0% { transform: scale(0.3); opacity: 0; } 60% { transform: scale(1.1); } 100% { transform: scale(1); opacity: 1; } }
        select { -webkit-appearance: none; }
        ::-webkit-scrollbar { width: 6px; } ::-webkit-scrollbar-track { background: transparent; } ::-webkit-scrollbar-thumb { background: ${SF.border}; border-radius: 3px; }
      `}</style>

      <div style={{ display: 'flex', minHeight: '100vh', fontFamily: tweaks.fontFamily + ', sans-serif' }}>
        {/* Sidebar */}
        {!isMobile && <SFSidebar step={step} user={formData.user} isMobile={false} />}
        {isMobile && <SFSidebar step={step} user={formData.user} isMobile={true} />}

        {/* Main content */}
        <div style={{ flex: 1, overflowY: 'auto', background: SF.bg }}>
          {isMobile && step === 5 && (
            <div style={contentStyle}>{renderScreen()}</div>
          )}
          {!(isMobile && step === 5) && (
            <div style={contentStyle}>{renderScreen()}</div>
          )}
        </div>

        {/* Tweaks panel */}
        {showTweaks && (
          <div style={{ position: 'fixed', bottom: 24, right: 24, width: 260, background: SF.white, borderRadius: 14, boxShadow: '0 8px 32px rgba(0,0,0,0.18)', padding: 20, zIndex: 9999, border: `1px solid ${SF.border}` }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: SF.navy, marginBottom: 14 }}>Tweaks</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <label style={{ fontSize: 11, fontWeight: 600, color: SF.muted, display: 'block', marginBottom: 5 }}>PRIMARY COLOUR</label>
                <div style={{ display: 'flex', gap: 6 }}>
                  {['#1B3A6B','#0F4C81','#1A3C34','#3B1F5E'].map(c => (
                    <div key={c} onClick={() => applyTweak('primaryColor', c)} style={{ width: 28, height: 28, borderRadius: 6, background: c, cursor: 'pointer', border: tweaks.primaryColor === c ? `3px solid ${SF.gold}` : '2px solid transparent' }} />
                  ))}
                </div>
              </div>
              <div>
                <label style={{ fontSize: 11, fontWeight: 600, color: SF.muted, display: 'block', marginBottom: 5 }}>ACCENT COLOUR</label>
                <div style={{ display: 'flex', gap: 6 }}>
                  {['#C8992B','#E07B39','#2E7D52','#9B59B6'].map(c => (
                    <div key={c} onClick={() => applyTweak('accentColor', c)} style={{ width: 28, height: 28, borderRadius: 6, background: c, cursor: 'pointer', border: tweaks.accentColor === c ? '3px solid #333' : '2px solid transparent' }} />
                  ))}
                </div>
              </div>
              <div>
                <label style={{ fontSize: 11, fontWeight: 600, color: SF.muted, display: 'block', marginBottom: 5 }}>FONT</label>
                <select value={tweaks.fontFamily} onChange={e => applyTweak('fontFamily', e.target.value)} style={{ width: '100%', padding: '7px 10px', border: `1px solid ${SF.border}`, borderRadius: 7, fontSize: 13, fontFamily: 'inherit' }}>
                  {['Plus Jakarta Sans','DM Sans','Inter','Nunito','Outfit'].map(f => <option key={f}>{f}</option>)}
                </select>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <input type="checkbox" id="compact" checked={tweaks.compactMode} onChange={e => applyTweak('compactMode', e.target.checked)} style={{ width: 15, height: 15, accentColor: SF.navy }} />
                <label htmlFor="compact" style={{ fontSize: 13, color: SF.text }}>Compact mode</label>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
