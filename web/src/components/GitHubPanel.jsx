import React, { useEffect, useState } from 'react';
import { collection, doc, setDoc, updateDoc } from 'firebase/firestore';
import { db } from '../firebase.js';
import { isSafeHttpUrl } from '../utils/safeUrl.js';

const SAFE_SEGMENT = /^[A-Za-z0-9._-]+$/;

function parseOwnerRepo(input) {
  if (!input) return '';
  let clean = input.trim();
  const qIdx = clean.indexOf('?');
  if (qIdx >= 0) clean = clean.slice(0, qIdx);
  const hIdx = clean.indexOf('#');
  if (hIdx >= 0) clean = clean.slice(0, hIdx);

  clean = clean.replace(/^https?:\/\/(www\.)?github\.com\//i, '').replace(/^github\.com\//i, '');
  clean = clean.replace(/\/+$/, '');
  if (clean.endsWith('.git')) clean = clean.slice(0, -4);
  clean = clean.replace(/\/+$/, '');
  const parts = clean.split('/');
  if (parts.length < 2) return '';
  const owner = parts[0].trim();
  const repo = parts[1].trim();
  if (!owner || !repo || owner === '.' || owner === '..' || repo === '.' || repo === '..') {
    return '';
  }
  if (!SAFE_SEGMENT.test(owner) || !SAFE_SEGMENT.test(repo)) return '';
  return `${owner}/${repo}`;
}

export default function GitHubPanel({ project }) {
  const [repoInput, setRepoInput] = useState(project?.githubRepoUrl || '');
  const [token, setToken] = useState(() => {
    try {
      localStorage.removeItem('projecthub_gh_token');
      return sessionStorage.getItem('projecthub_gh_token') || '';
    } catch {
      return '';
    }
  });
  const [repoInfo, setRepoInfo] = useState(null);
  const [items, setItems] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setRepoInput(project?.githubRepoUrl || '');
    if (project?.githubRepoUrl) {
      fetchGitHub(project.githubRepoUrl, token);
    }
  }, [project?.githubRepoUrl]);

  async function fetchGitHub(urlOrSlug, pat) {
    const slug = parseOwnerRepo(urlOrSlug);
    if (!slug) {
      setError('Enter a valid GitHub repo (owner/repo or https://github.com/owner/repo).');
      return;
    }
    setLoading(true);
    setError('');
    const headers = { Accept: 'application/vnd.github+json' };
    if (pat?.trim()) headers.Authorization = `Bearer ${pat.trim()}`;

    try {
      const [repoRes, issuesRes] = await Promise.all([
        fetch(`https://api.github.com/repos/${slug}`, { headers }),
        fetch(`https://api.github.com/repos/${slug}/issues?state=all&per_page=30`, { headers }),
      ]);
      if (!repoRes.ok) throw new Error(`GitHub repo fetch failed (${repoRes.status})`);
      if (!issuesRes.ok) throw new Error(`GitHub issues fetch failed (${issuesRes.status})`);

      const repoJson = await repoRes.json();
      const issuesJson = await issuesRes.json();
      setRepoInfo(repoJson);
      setItems(Array.isArray(issuesJson) ? issuesJson : []);
    } catch (err) {
      setError(err.message || 'GitHub sync failed.');
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveRepo(e) {
    e.preventDefault();
    const slug = parseOwnerRepo(repoInput);
    if (!slug) {
      setError('Enter a valid GitHub repo (owner/repo or https://github.com/owner/repo).');
      return;
    }
    try {
      await updateDoc(doc(db, 'projects', project.id), {
        githubRepoUrl: repoInput.trim(),
      });
      fetchGitHub(repoInput, token);
    } catch (err) {
      setError(err.message || 'Failed to save repository.');
    }
  }

  function handleSaveToken(val) {
    setToken(val);
    try {
      localStorage.removeItem('projecthub_gh_token');
      if (val.trim()) {
        sessionStorage.setItem('projecthub_gh_token', val.trim());
      } else {
        sessionStorage.removeItem('projecthub_gh_token');
      }
    } catch {
      // Ignore storage errors
    }
  }

  async function importAsTask(item) {
    const isPr = Boolean(item.pull_request);
    try {
      const taskRef = doc(collection(db, 'projects', project.id, 'tasks'));
      await setDoc(taskRef, {
        taskId: taskRef.id,
        projectId: project.id,
        title: `${isPr ? '[PR #' : '[GH #'}${item.number}] ${item.title}`,
        description: `Imported from GitHub: ${isSafeHttpUrl(item.html_url) ? item.html_url : ''}`,
        status: item.state === 'closed' ? 'DONE' : 'TODO',
        priority: 'MEDIUM',
        issueType: isPr ? 'FEATURE' : 'BUG',
        phase: 'DEVELOPMENT',
        taskKey: `GH-${item.number}`,
        githubIssueNumber: item.number,
        githubIssueUrl: isSafeHttpUrl(item.html_url) ? item.html_url : '',
        assignedTo: '',
        assignedToName: '',
        dueDate: Date.now() + 7 * 86400000,
        subtasks: [],
        tags: [],
        commentsCount: 0,
        timeSpentHours: 0,
        startDate: Date.now(),
      });
      alert(`Imported #${item.number} into ProjectHub Tasks!`);
    } catch (err) {
      alert(`Failed to import item: ${err.message}`);
    }
  }

  const filtered = items.filter((item) => {
    const isPr = Boolean(item.pull_request);
    if (filter === 'ISSUES' && isPr) return false;
    if (filter === 'PRS' && !isPr) return false;
    if (filter === 'OPEN' && item.state !== 'open') return false;
    return true;
  });

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <strong>GitHub Issues & Pull Requests</strong>
        {repoInfo && (
          <span className="pill">
            {repoInfo.full_name} • ★ {repoInfo.stargazers_count}
          </span>
        )}
      </div>

      <form onSubmit={handleSaveRepo} className="row" style={{ marginBottom: 10 }}>
        <input
          className="input"
          style={{ flex: 2 }}
          placeholder="https://github.com/owner/repo"
          value={repoInput}
          onChange={(e) => setRepoInput(e.target.value)}
        />
        <input
          className="input"
          style={{ flex: 1 }}
          type="password"
          placeholder="Optional GitHub PAT"
          value={token}
          onChange={(e) => handleSaveToken(e.target.value)}
        />
        <button type="submit" className="btn btn-primary">
          Save & Sync
        </button>
      </form>

      <div className="tabs" style={{ marginTop: 8 }}>
        {['ALL', 'ISSUES', 'PRS', 'OPEN'].map((f) => (
          <button
            key={f}
            type="button"
            className={`tab-btn ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {f}
          </button>
        ))}
      </div>

      {loading && <p className="muted">Syncing with GitHub...</p>}
      {error && <p style={{ color: 'var(--danger)' }}>{error}</p>}

      {filtered.map((item) => {
        const isPr = Boolean(item.pull_request);
        return (
          <div
            key={item.id}
            className="row"
            style={{
              justifyContent: 'space-between',
              padding: '10px 0',
              borderBottom: '1px solid var(--border)',
            }}
          >
            <div>
              <span className="pill" style={{ marginRight: 8 }}>
                {isPr ? 'PR' : 'ISSUE'} #{item.number} ({item.state})
              </span>
              <strong>{item.title}</strong>
              <div className="muted">@{item.user?.login}</div>
            </div>
            <div className="row">
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => importAsTask(item)}
              >
                + Import Task
              </button>
              {isSafeHttpUrl(item.html_url) && (
                <a
                  className="btn btn-sm"
                  href={item.html_url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Open
                </a>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
