import React, { useEffect, useState } from 'react';

/**
 * Web Settings panel for theme preference, invite-code joining, GitHub PAT, and logout.
 */
export default function SettingsPanel({
  user,
  themePreference,
  resolvedTheme,
  onChangeTheme,
  onJoinProjectByCode,
  onLogout,
}) {
  const [joinCode, setJoinCode] = useState('');
  const [joinStatus, setJoinStatus] = useState('');
  const [joinError, setJoinError] = useState('');
  const [joining, setJoining] = useState(false);

  const [ghToken, setGhToken] = useState(() => {
    try {
      return sessionStorage.getItem('projecthub_gh_token') || '';
    } catch {
      return '';
    }
  });
  const [hasSavedToken, setHasSavedToken] = useState(() => {
    try {
      return Boolean(sessionStorage.getItem('projecthub_gh_token'));
    } catch {
      return false;
    }
  });
  const [tokenMsg, setTokenMsg] = useState('');

  useEffect(() => {
    try {
      const stored = sessionStorage.getItem('projecthub_gh_token') || '';
      setGhToken(stored);
      setHasSavedToken(Boolean(stored));
    } catch {
      setHasSavedToken(false);
    }
  }, []);

  async function handleJoinSubmit(e) {
    e.preventDefault();
    const cleaned = joinCode.trim().toUpperCase();
    if (!cleaned) {
      setJoinError('Enter a valid join code.');
      setJoinStatus('');
      return;
    }
    setJoining(true);
    setJoinError('');
    setJoinStatus('');
    try {
      const res = await onJoinProjectByCode(cleaned);
      if (res?.ok) {
        setJoinStatus(res.message || 'Joined workspace.');
        setJoinCode('');
      } else {
        setJoinError(res?.message || 'Unable to join workspace.');
      }
    } catch (err) {
      setJoinError(err?.message || 'Unable to join workspace.');
    } finally {
      setJoining(false);
    }
  }

  function handleSaveToken(e) {
    e.preventDefault();
    const cleaned = ghToken.trim();
    try {
      localStorage.removeItem('projecthub_gh_token');
      if (cleaned) {
        sessionStorage.setItem('projecthub_gh_token', cleaned);
        setHasSavedToken(true);
        setTokenMsg('GitHub token saved for this browser session.');
      } else {
        sessionStorage.removeItem('projecthub_gh_token');
        setHasSavedToken(false);
        setTokenMsg('GitHub token cleared.');
      }
    } catch {
      setTokenMsg('Session storage is unavailable in this browser.');
    }
  }

  function handleClearToken() {
    try {
      localStorage.removeItem('projecthub_gh_token');
      sessionStorage.removeItem('projecthub_gh_token');
    } catch {
      // Ignore storage errors
    }
    setGhToken('');
    setHasSavedToken(false);
    setTokenMsg('GitHub token cleared.');
  }

  const themeOptions = [
    { id: 'light', label: 'Light' },
    { id: 'dark', label: 'Dark' },
    { id: 'system', label: 'System' },
  ];

  return (
    <div style={{ display: 'grid', gap: 16, maxWidth: 820, margin: '0 auto' }}>
      <div className="card">
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 10 }}>
          <div>
            <h3 style={{ margin: '0 0 4px' }}>Appearance</h3>
            <p className="muted" style={{ margin: 0 }}>
              Currently using <strong>{resolvedTheme === 'dark' ? 'Dark' : 'Light'}</strong> theme
              {themePreference === 'system' ? ' (System default)' : ''}.
            </p>
          </div>
          <div className="row">
            {themeOptions.map((opt) => (
              <button
                key={opt.id}
                type="button"
                className={`btn btn-sm ${themePreference === opt.id ? 'btn-primary' : ''}`}
                onClick={() => onChangeTheme(opt.id)}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="card">
        <h3 style={{ margin: '0 0 6px' }}>Join Project via Code</h3>
        <p className="muted" style={{ margin: '0 0 12px' }}>
          Enter an invite code shared by a Team Lead to join their workspace directly.
        </p>
        <form onSubmit={handleJoinSubmit} className="row">
          <input
            className="input"
            style={{ flex: 1, minWidth: 200, textTransform: 'uppercase' }}
            placeholder="Invite Code (e.g. AB12CD34)"
            maxLength={12}
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value)}
          />
          <button type="submit" className="btn btn-primary btn-sm" disabled={joining}>
            {joining ? 'Joining...' : 'Join Workspace'}
          </button>
        </form>
        {joinError && (
          <div style={{ color: 'var(--danger)', fontSize: 13, marginTop: 8 }}>{joinError}</div>
        )}
        {joinStatus && (
          <div style={{ color: 'var(--success)', fontSize: 13, marginTop: 8 }}>{joinStatus}</div>
        )}
      </div>

      <div className="card">
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 8 }}>
          <div>
            <h3 style={{ margin: '0 0 4px' }}>GitHub Integration</h3>
            <p className="muted" style={{ margin: 0 }}>
              Stored in session storage for higher API limits and private repositories. Cleared on logout.
            </p>
          </div>
          <span className={`pill ${hasSavedToken ? 'pill-accent' : ''}`}>
            {hasSavedToken ? 'Token saved for session' : 'Not configured'}
          </span>
        </div>
        <form onSubmit={handleSaveToken} className="row" style={{ marginTop: 10 }}>
          <input
            className="input"
            type="password"
            style={{ flex: 1, minWidth: 220 }}
            placeholder="GitHub Personal Access Token (ghp_...)"
            value={ghToken}
            onChange={(e) => setGhToken(e.target.value)}
          />
          <button type="submit" className="btn btn-primary btn-sm">
            Save Token
          </button>
          <button type="button" className="btn btn-sm" onClick={handleClearToken}>
            Clear Token
          </button>
        </form>
        {tokenMsg && <div className="muted" style={{ marginTop: 8 }}>{tokenMsg}</div>}
      </div>

      <div className="card">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <h3 style={{ margin: '0 0 4px' }}>Account &amp; Session</h3>
            <div style={{ fontWeight: 600 }}>{user?.displayName || user?.email || 'Campus User'}</div>
            <div className="muted">{user?.email || ''}</div>
          </div>
          <button type="button" className="btn btn-danger" onClick={onLogout}>
            Log Out
          </button>
        </div>
      </div>
    </div>
  );
}
