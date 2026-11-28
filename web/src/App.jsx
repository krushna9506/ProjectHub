import React, { useEffect, useState } from 'react';
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth';
import {
  addDoc,
  collection,
  doc,
  getDoc,
  onSnapshot,
  orderBy,
  query,
  setDoc,
  updateDoc,
  where,
} from 'firebase/firestore';
import { auth, db, isFirebaseConfigured } from './firebase.js';
import KanbanBoard from './components/KanbanBoard.jsx';
import TeamChat from './components/TeamChat.jsx';
import CalendarRoadmap from './components/CalendarRoadmap.jsx';
import GitHubPanel from './components/GitHubPanel.jsx';
import FilesPanel from './components/FilesPanel.jsx';
import ExploreCampus from './components/ExploreCampus.jsx';
import MessagesPanel, { isConversationUnread } from './components/MessagesPanel.jsx';
import SettingsPanel from './components/SettingsPanel.jsx';

const TEMPLATES = {
  BLANK: { category: 'General', days: 30, lookingFor: '' },
  CAPSTONE: { category: 'Capstone', days: 90, lookingFor: 'Backend, Frontend, Docs' },
  HACKATHON: { category: 'SIH Hackathon', days: 3, lookingFor: 'Fullstack, UI/UX, Pitch' },
  RESEARCH: { category: 'Research', days: 60, lookingFor: 'ML Researcher, Technical Writer' },
};

function generateJoinCode(length = 8) {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  const buf = new Uint32Array(length);
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    crypto.getRandomValues(buf);
  } else {
    for (let i = 0; i < length; i++) {
      buf[i] = Math.floor(Math.random() * chars.length);
    }
  }
  let code = '';
  for (let i = 0; i < length; i++) {
    code += chars[buf[i] % chars.length];
  }
  return code;
}

function BrandLogo() {
  return (
    <span className="brand-mark" aria-hidden="true">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="9" rx="1.5" />
        <rect x="14" y="3" width="7" height="5" rx="1.5" />
        <rect x="14" y="12" width="7" height="9" rx="1.5" />
        <rect x="3" y="16" width="7" height="5" rx="1.5" />
      </svg>
    </span>
  );
}

function ThemeToggleButton({ theme, onToggle }) {
  return (
    <button
      type="button"
      className="btn btn-sm"
      onClick={onToggle}
      aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
      title={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
    >
      {theme === 'dark' ? (
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
        </svg>
      ) : (
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
        </svg>
      )}
      {theme === 'dark' ? 'Light' : 'Dark'}
    </button>
  );
}

export default function App() {
  const [themePreference, setThemePreference] = useState(() => {
    try {
      const saved = localStorage.getItem('projecthub_theme');
      if (saved === 'dark' || saved === 'light' || saved === 'system') return saved;
      return 'system';
    } catch {
      return 'system';
    }
  });
  const [systemPrefersDark, setSystemPrefersDark] = useState(() => {
    try {
      return Boolean(window.matchMedia?.('(prefers-color-scheme: dark)').matches);
    } catch {
      return false;
    }
  });
  const [user, setUser] = useState(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');

  const [projects, setProjects] = useState([]);
  const [publicProjects, setPublicProjects] = useState([]);
  const [campusUsers, setCampusUsers] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [activeTab, setActiveTab] = useState('KANBAN');
  const [viewMode, setViewMode] = useState('WORKSPACE'); // 'WORKSPACE' | 'MESSAGES' | 'EXPLORE' | 'SETTINGS'
  const [conversations, setConversations] = useState([]);
  const [selectedConversationId, setSelectedConversationId] = useState('');

  const [tasks, setTasks] = useState([]);
  const [selectedTask, setSelectedTask] = useState(null);
  const [messages, setMessages] = useState([]);
  const [milestones, setMilestones] = useState([]);
  const [automations, setAutomations] = useState([]);
  const [files, setFiles] = useState([]);
  const [members, setMembers] = useState([]);

  const [newProjTitle, setNewProjTitle] = useState('');
  const [newProjDesc, setNewProjDesc] = useState('');
  const [newProjTemplate, setNewProjTemplate] = useState('CAPSTONE');

  const resolvedTheme =
    themePreference === 'system' ? (systemPrefersDark ? 'dark' : 'light') : themePreference;

  useEffect(() => {
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!mq) return undefined;
    const handleMediaChange = (e) => setSystemPrefersDark(e.matches);
    setSystemPrefersDark(mq.matches);
    mq.addEventListener?.('change', handleMediaChange);
    return () => mq.removeEventListener?.('change', handleMediaChange);
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', resolvedTheme);
    try {
      localStorage.setItem('projecthub_theme', themePreference);
    } catch {
      // Ignore storage failures
    }
  }, [themePreference, resolvedTheme]);

  const toggleTheme = () =>
    setThemePreference(resolvedTheme === 'dark' ? 'light' : 'dark');

  function clearSensitiveState() {
    setProjects([]);
    setPublicProjects([]);
    setCampusUsers([]);
    setConversations([]);
    setSelectedProjectId('');
    setSelectedConversationId('');
    setTasks([]);
    setSelectedTask(null);
    setMessages([]);
    setMilestones([]);
    setAutomations([]);
    setFiles([]);
    setMembers([]);
    try {
      sessionStorage.removeItem('projecthub_gh_token');
      localStorage.removeItem('projecthub_gh_token');
    } catch {
      // Ignore storage errors
    }
  }

  useEffect(() => {
    if (!isFirebaseConfigured || !auth) return;
    return onAuthStateChanged(auth, (fbUser) => {
      setUser(fbUser);
      if (!fbUser) {
        clearSensitiveState();
      }
    });
  }, []);

  useEffect(() => {
    if (!user || !db) return;
    const unsubProjects = onSnapshot(
      query(collection(db, 'projects'), where(`members.${user.uid}`, '>', '')),
      (snap) => {
        const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        setProjects(list);
        if (!selectedProjectId && list.length > 0) {
          setSelectedProjectId(list[0].id);
        }
      }
    );

    const unsubPublic = onSnapshot(
      query(collection(db, 'projects'), where('isPublic', '==', true)),
      (snap) => {
        const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        setPublicProjects(list);
      }
    );

    const unsubUsers = onSnapshot(collection(db, 'users'), (snap) => {
      setCampusUsers(snap.docs.map((d) => ({ userId: d.id, ...d.data() })));
    });

    const unsubConversations = onSnapshot(
      query(collection(db, 'conversations'), where('participantIds', 'array-contains', user.uid)),
      (snap) => {
        const list = snap.docs
          .map((d) => ({ id: d.id, ...d.data() }))
          .sort(
            (a, b) =>
              Math.max(b.lastMessageTimestamp || 0, b.createdAt || 0) -
              Math.max(a.lastMessageTimestamp || 0, a.createdAt || 0)
          );
        setConversations(list);
      }
    );

    return () => {
      unsubProjects();
      unsubPublic();
      unsubUsers();
      unsubConversations();
    };
  }, [user]);

  const currentProject = projects.find((p) => p.id === selectedProjectId) || null;

  useEffect(() => {
    if (!currentProject || !db || !user) return;
    const pid = currentProject.id;

    const unsubTasks = onSnapshot(collection(db, 'projects', pid, 'tasks'), (snap) => {
      const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      setTasks(list);
      if (selectedTask) {
        const refreshed = list.find((t) => t.id === selectedTask.id);
        setSelectedTask(refreshed || null);
      }
    });

    const unsubMsgs = onSnapshot(
      query(collection(db, 'projects', pid, 'messages'), orderBy('timestamp', 'asc')),
      (snap) => setMessages(snap.docs.map((d) => ({ id: d.id, ...d.data() })))
    );

    const unsubMilestones = onSnapshot(
      query(collection(db, 'projects', pid, 'milestones'), orderBy('dueDate', 'asc')),
      (snap) => setMilestones(snap.docs.map((d) => ({ id: d.id, ...d.data() })))
    );

    const unsubRules = onSnapshot(
      collection(db, 'projects', pid, 'automations'),
      (snap) => setAutomations(snap.docs.map((d) => ({ id: d.id, ...d.data() })))
    );

    const isLead = currentProject.leadId === user.uid;
    let unsubFiles = () => {};
    let unsubOwnFiles = () => {};

    if (isLead) {
      unsubFiles = onSnapshot(
        query(collection(db, 'projects', pid, 'files'), orderBy('timestamp', 'desc')),
        (snap) => setFiles(snap.docs.map((d) => ({ id: d.id, ...d.data() })))
      );
    } else {
      let shared = [];
      let own = [];
      const emit = () => {
        const map = new Map();
        [...shared, ...own].forEach((item) => map.set(item.id, item));
        const merged = Array.from(map.values()).sort(
          (a, b) => (b.timestamp || 0) - (a.timestamp || 0)
        );
        setFiles(merged);
      };
      unsubFiles = onSnapshot(
        query(
          collection(db, 'projects', pid, 'files'),
          where('accessLevel', 'in', ['MEMBERS_ONLY', 'PUBLIC'])
        ),
        (snap) => {
          shared = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
          emit();
        }
      );
      unsubOwnFiles = onSnapshot(
        query(collection(db, 'projects', pid, 'files'), where('uploadedById', '==', user.uid)),
        (snap) => {
          own = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
          emit();
        }
      );
    }

    const memberIds = Object.keys(currentProject.members || {});
    Promise.all(memberIds.map((uid) => getDoc(doc(db, 'users', uid)))).then((snaps) => {
      setMembers(
        snaps.filter((s) => s.exists()).map((s) => ({ userId: s.id, ...s.data() }))
      );
    });

    return () => {
      unsubTasks();
      unsubMsgs();
      unsubMilestones();
      unsubRules();
      unsubFiles();
      unsubOwnFiles();
    };
  }, [currentProject?.id, user?.uid]);

  async function openOrCreateDirectChat(otherUser) {
    if (!user || !otherUser?.userId || otherUser.userId === user.uid || !db) return;
    const myUid = user.uid;
    const otherUid = otherUser.userId;
    const sorted = myUid < otherUid ? [myUid, otherUid] : [otherUid, myUid];
    const dmId = `dm_${sorted[0]}_${sorted[1]}`;
    const convRef = doc(db, 'conversations', dmId);
    const snap = await getDoc(convRef);
    if (!snap.exists()) {
      const now = Date.now();
      const myName = user.displayName || user.email?.split('@')[0] || 'Member';
      const otherName =
        otherUser.name || (otherUser.email ? otherUser.email.split('@')[0] : 'Member');
      await setDoc(convRef, {
        conversationId: dmId,
        type: 'DIRECT',
        title: '',
        description: '',
        createdBy: myUid,
        participantIds: sorted,
        participantNames: { [myUid]: myName, [otherUid]: otherName },
        participantEmails: { [myUid]: user.email || '', [otherUid]: otherUser.email || '' },
        lastMessage: '',
        lastMessageSenderId: '',
        lastMessageSenderName: '',
        lastMessageTimestamp: now,
        createdAt: now,
        readBy: { [myUid]: now },
      });
    }
    setSelectedConversationId(dmId);
    setViewMode('MESSAGES');
  }

  const unreadConversationsCount = conversations.filter((c) =>
    isConversationUnread(c, user?.uid)
  ).length;

  async function handleLogin(e) {
    e.preventDefault();
    setAuthError('');
    try {
      await signInWithEmailAndPassword(auth, email.trim(), password);
    } catch (err) {
      setAuthError(err.message || 'Sign in failed');
    }
  }

  async function handleCreateProject(e) {
    e.preventDefault();
    if (!newProjTitle.trim()) return;
    const spec = TEMPLATES[newProjTemplate] || TEMPLATES.BLANK;
    const joinCode = generateJoinCode(8);
    const projRef = doc(collection(db, 'projects'));
    const now = Date.now();

    await setDoc(projRef, {
      projectId: projRef.id,
      title: newProjTitle.trim(),
      description: newProjDesc.trim(),
      joinCode,
      leadId: user.uid,
      leadName: user.displayName || user.email?.split('@')[0] || 'Lead',
      members: { [user.uid]: 'LEAD' },
      progressPercentage: 0,
      createdAt: now,
      deadline: now + spec.days * 86400000,
      isPublic: true,
      public: true,
      category: spec.category,
      lookingFor: spec.lookingFor,
      templateType: newProjTemplate,
      customStatuses: ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'],
    });
    await setDoc(doc(db, 'join_codes', joinCode), {
      joinCode,
      projectId: projRef.id,
      leadId: user.uid,
      createdAt: now,
    });
    setSelectedProjectId(projRef.id);
    setNewProjTitle('');
    setNewProjDesc('');
  }

  async function handleJoinProjectByCode(rawCode) {
    if (!user || !db) return { ok: false, message: 'Sign in first.' };
    const code = (rawCode || '').trim().toUpperCase();
    if (!code) return { ok: false, message: 'Enter an invite code.' };

    const codeSnap = await getDoc(doc(db, 'join_codes', code));
    if (!codeSnap.exists()) {
      return { ok: false, message: 'Invalid or expired invite code.' };
    }
    const codeData = codeSnap.data() || {};
    const projectId = codeData.projectId;
    if (!projectId) {
      return { ok: false, message: 'Invite code has no linked project.' };
    }
    if (projects.some((p) => p.id === projectId)) {
      setSelectedProjectId(projectId);
      return { ok: true, message: 'You are already a member of this workspace.' };
    }
    await updateDoc(doc(db, 'projects', projectId), {
      [`members.${user.uid}`]: 'MEMBER',
      lastJoinCodeUsed: `${code}:${user.uid}`,
    });
    setSelectedProjectId(projectId);
    return { ok: true, message: 'Joined workspace successfully!' };
  }

  async function handleLogout() {
    clearSensitiveState();
    await signOut(auth);
  }

  if (!isFirebaseConfigured) {
    return (
      <div className="auth-shell">
        <div className="auth-card">
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 14 }}>
            <div className="brand">
              <BrandLogo />
              <span>ProjectHub</span>
            </div>
            <ThemeToggleButton theme={resolvedTheme} onToggle={toggleTheme} />
          </div>
          <h2 style={{ marginTop: 0, marginBottom: 8 }}>Web Companion Setup</h2>
          <p className="muted" style={{ marginBottom: 0 }}>
            Copy <code>web/.env.example</code> to <code>web/.env</code> and add your Firebase
            project config to connect to the shared Android + Web Firestore database.
          </p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="auth-shell">
        <div className="auth-card">
          <div className="row" style={{ justifyContent: 'space-between', marginBottom: 16 }}>
            <div className="brand">
              <BrandLogo />
              <span>ProjectHub</span>
            </div>
            <ThemeToggleButton theme={resolvedTheme} onToggle={toggleTheme} />
          </div>
          <h2 style={{ margin: '0 0 6px', fontSize: 22 }}>Welcome back</h2>
          <p className="muted" style={{ margin: '0 0 18px' }}>
            Sign in with your ProjectHub student or mentor account.
          </p>
          <form onSubmit={handleLogin} style={{ display: 'grid', gap: 12 }}>
            <input
              className="input"
              type="email"
              placeholder="Campus email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <input
              className="input"
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            {authError && <div style={{ color: 'var(--danger)', fontSize: 13 }}>{authError}</div>}
            <button type="submit" className="btn btn-primary" style={{ minHeight: 42 }}>
              Sign In
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div>
      <header className="topbar">
        <div className="brand">
          <BrandLogo />
          <span>ProjectHub</span>
          <span className="brand-badge">WEB</span>
        </div>
        <div className="row">
          <button
            className={`btn btn-sm ${viewMode === 'WORKSPACE' ? 'btn-primary' : ''}`}
            onClick={() => setViewMode('WORKSPACE')}
          >
            Workspace
          </button>
          <button
            className={`btn btn-sm ${viewMode === 'MESSAGES' ? 'btn-primary' : ''}`}
            onClick={() => {
              setViewMode('MESSAGES');
              if (!selectedConversationId && conversations.length > 0) {
                setSelectedConversationId(conversations[0].id);
              }
            }}
          >
            Messages
            {unreadConversationsCount > 0 && (
              <span className="pill pill-accent" style={{ padding: '1px 6px', fontSize: 10 }}>
                {unreadConversationsCount}
              </span>
            )}
          </button>
          <button
            className={`btn btn-sm ${viewMode === 'EXPLORE' ? 'btn-primary' : ''}`}
            onClick={() => setViewMode('EXPLORE')}
          >
            Campus Explore
          </button>
          <button
            className={`btn btn-sm ${viewMode === 'SETTINGS' ? 'btn-primary' : ''}`}
            onClick={() => setViewMode('SETTINGS')}
          >
            Settings
          </button>
        </div>
      </header>

      <div className="container">
        {viewMode === 'MESSAGES' ? (
          <MessagesPanel
            conversations={conversations}
            selectedConversationId={selectedConversationId}
            onSelectConversation={setSelectedConversationId}
            campusUsers={campusUsers}
            currentUser={user}
            onOpenDirectChat={openOrCreateDirectChat}
          />
        ) : viewMode === 'EXPLORE' ? (
          <ExploreCampus
            publicProjects={publicProjects}
            campusUsers={campusUsers}
            myProjects={projects}
            currentUser={user}
            onMessageUser={openOrCreateDirectChat}
          />
        ) : viewMode === 'SETTINGS' ? (
          <SettingsPanel
            user={user}
            themePreference={themePreference}
            resolvedTheme={resolvedTheme}
            onChangeTheme={setThemePreference}
            onJoinProjectByCode={handleJoinProjectByCode}
            onLogout={handleLogout}
          />
        ) : (
          <>
            <div className="card" style={{ marginBottom: 16 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div className="row">
                  <strong>Workspace:</strong>
                  <select
                    className="select"
                    style={{ width: 260 }}
                    value={selectedProjectId}
                    onChange={(e) => setSelectedProjectId(e.target.value)}
                  >
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.title} ({p.progressPercentage || 0}%)
                      </option>
                    ))}
                  </select>
                  {currentProject && (
                    <>
                      <span className="pill">Code: {currentProject.joinCode}</span>
                      <span className="pill pill-accent">
                        {currentProject.progressPercentage || 0}% done
                      </span>
                    </>
                  )}
                </div>

                <form onSubmit={handleCreateProject} className="row">
                  <select
                    className="select"
                    style={{ width: 140 }}
                    value={newProjTemplate}
                    onChange={(e) => setNewProjTemplate(e.target.value)}
                  >
                    <option value="CAPSTONE">Capstone</option>
                    <option value="HACKATHON">Hackathon</option>
                    <option value="RESEARCH">Research</option>
                    <option value="BLANK">Blank</option>
                  </select>
                  <input
                    className="input"
                    style={{ width: 180 }}
                    placeholder="New project title"
                    value={newProjTitle}
                    onChange={(e) => setNewProjTitle(e.target.value)}
                  />
                  <input
                    className="input"
                    style={{ width: 180 }}
                    placeholder="Description"
                    value={newProjDesc}
                    onChange={(e) => setNewProjDesc(e.target.value)}
                  />
                  <button type="submit" className="btn btn-primary btn-sm">
                    + New Project
                  </button>
                </form>
              </div>
            </div>

            {currentProject ? (
              <>
                <div className="tabs">
                  {[
                    ['KANBAN', 'Kanban Board'],
                    ['CHAT', 'Team Chat'],
                    ['ROADMAP', 'Calendar & Automations'],
                    ['GITHUB', 'GitHub Issues & PRs'],
                    ['FILES', 'Files & Snippets'],
                  ].map(([key, label]) => (
                    <button
                      key={key}
                      className={`tab-btn ${activeTab === key ? 'active' : ''}`}
                      onClick={() => setActiveTab(key)}
                    >
                      {label}
                    </button>
                  ))}
                </div>

                {activeTab === 'KANBAN' && (
                  <KanbanBoard
                    project={currentProject}
                    tasks={tasks}
                    members={members}
                    currentUser={user}
                    onSelectTask={setSelectedTask}
                    selectedTask={selectedTask}
                  />
                )}
                {activeTab === 'CHAT' && (
                  <TeamChat
                    project={currentProject}
                    messages={messages}
                    members={members}
                    currentUser={user}
                  />
                )}
                {activeTab === 'ROADMAP' && (
                  <CalendarRoadmap
                    project={currentProject}
                    tasks={tasks}
                    milestones={milestones}
                    automations={automations}
                  />
                )}
                {activeTab === 'GITHUB' && <GitHubPanel project={currentProject} />}
                {activeTab === 'FILES' && (
                  <FilesPanel project={currentProject} files={files} currentUser={user} />
                )}
              </>
            ) : (
              <div className="card">
                <p className="muted">Create a project above or explore campus projects to join.</p>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
