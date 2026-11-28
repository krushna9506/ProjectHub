import React, { useMemo, useState } from 'react';
import { collection, doc, setDoc } from 'firebase/firestore';
import { db } from '../firebase.js';

export default function ExploreCampus({
  publicProjects,
  campusUsers,
  myProjects,
  currentUser,
  onMessageUser,
}) {
  const [tab, setTab] = useState('PROJECTS');
  const [queryText, setQueryText] = useState('');
  const [peopleFilter, setPeopleFilter] = useState('ALL');

  const filteredProjects = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    return (publicProjects || []).filter((p) => {
      if (!q) return true;
      return `${p.title || ''} ${p.description || ''} ${p.category || ''} ${p.lookingFor || ''}`
        .toLowerCase()
        .includes(q);
    });
  }, [publicProjects, queryText]);

  const filteredUsers = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    return (campusUsers || []).filter((u) => {
      if (peopleFilter === 'MENTORS' && u.role !== 'FACULTY_MENTOR') return false;
      if (peopleFilter === 'VERIFIED' && !u.campusVerified) return false;
      if (!q) return true;
      const skills = (u.skills || []).join(' ');
      return `${u.name || ''} ${u.department || ''} ${skills} ${u.collegeDomain || ''}`
        .toLowerCase()
        .includes(q);
    });
  }, [campusUsers, queryText, peopleFilter]);

  async function requestToJoin(project) {
    try {
      const reqRef = doc(collection(db, 'join_requests'));
      await setDoc(reqRef, {
        requestId: reqRef.id,
        projectId: project.id,
        projectTitle: project.title,
        leadId: project.leadId,
        applicantId: currentUser.uid,
        applicantName: currentUser.displayName || currentUser.email,
        applicantEmail: currentUser.email || '',
        message: 'Requesting to join via ProjectHub Web Companion',
        status: 'PENDING',
        timestamp: Date.now(),
      });
      const notifRef = doc(collection(db, 'users', project.leadId, 'notifications'));
      await setDoc(notifRef, {
        notificationId: notifRef.id,
        userId: project.leadId,
        title: 'New Team Join Request',
        message: `${currentUser.displayName || currentUser.email} requested to join ${project.title}`,
        type: 'JOIN_REQUEST',
        projectId: project.id,
        projectTitle: project.title,
        timestamp: Date.now(),
        read: false,
      });
      alert('Join request sent!');
    } catch (err) {
      alert(`Failed to send request: ${err.message}`);
    }
  }

  async function inviteUser(user) {
    if (!myProjects.length) {
      alert('Create or join a project first.');
      return;
    }
    const targetProject = myProjects[0];
    try {
      const notifRef = doc(collection(db, 'users', user.userId, 'notifications'));
      await setDoc(notifRef, {
        notificationId: notifRef.id,
        userId: user.userId,
        title: 'Project Invitation',
        message: `${currentUser.displayName || currentUser.email} invited you to ${targetProject.title}. Join Code: ${targetProject.joinCode}`,
        type: 'TEAM_INVITE',
        projectId: targetProject.id,
        projectTitle: targetProject.title,
        timestamp: Date.now(),
        read: false,
      });
      alert(`Invitation sent to ${user.name} for ${targetProject.title}`);
    } catch (err) {
      alert(`Failed to send invitation: ${err.message}`);
    }
  }

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <div className="tabs" style={{ margin: 0 }}>
          <button
            className={`tab-btn ${tab === 'PROJECTS' ? 'active' : ''}`}
            onClick={() => setTab('PROJECTS')}
          >
            Public Projects
          </button>
          <button
            className={`tab-btn ${tab === 'PEOPLE' ? 'active' : ''}`}
            onClick={() => setTab('PEOPLE')}
          >
            Teammates & Faculty Mentors
          </button>
        </div>

        <input
          className="input"
          style={{ width: 260 }}
          placeholder="Search projects, skills, mentors..."
          value={queryText}
          onChange={(e) => setQueryText(e.target.value)}
        />
      </div>

      {tab === 'PEOPLE' && (
        <div className="tabs">
          {['ALL', 'MENTORS', 'VERIFIED'].map((f) => (
            <button
              key={f}
              className={`tab-btn ${peopleFilter === f ? 'active' : ''}`}
              onClick={() => setPeopleFilter(f)}
            >
              {f}
            </button>
          ))}
        </div>
      )}

      {tab === 'PROJECTS' ? (
        <div className="grid-2">
          {filteredProjects.map((p) => {
            const isMember =
              p.leadId === currentUser.uid || Boolean(p.members?.[currentUser.uid]);
            return (
              <div className="task-card" key={p.id}>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>{p.title}</strong>
                  <span className="pill">{p.category || 'GENERAL'}</span>
                </div>
                <p className="muted">{p.description}</p>
                {p.lookingFor && <div className="muted">Looking for: {p.lookingFor}</div>}
                <div style={{ marginTop: 8 }}>
                  {isMember ? (
                    <span className="pill">Already a Member</span>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => requestToJoin(p)}
                    >
                      Request to Join
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="grid-2">
          {filteredUsers.map((u) => (
            <div className="task-card" key={u.userId}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong>
                  {u.name} {u.campusVerified ? '✓' : ''}
                </strong>
                <span className="pill">{u.role || 'STUDENT'}</span>
              </div>
              <div className="muted">
                {u.department || ''} {u.collegeDomain ? `• ${u.collegeDomain}` : ''}
              </div>
              {u.skills?.length > 0 && (
                <div className="muted" style={{ marginTop: 4 }}>
                  Skills: {u.skills.join(', ')}
                </div>
              )}
              {u.userId !== currentUser.uid && (
                <div className="row" style={{ marginTop: 8 }}>
                  {onMessageUser && (
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      onClick={() => onMessageUser(u)}
                    >
                      Message
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => inviteUser(u)}
                  >
                    Invite to Project
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
