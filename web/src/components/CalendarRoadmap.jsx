import React, { useMemo, useState } from 'react';
import { addDoc, collection, deleteDoc, doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebase.js';

export default function CalendarRoadmap({ project, tasks, milestones, automations }) {
  const [msTitle, setMsTitle] = useState('');
  const [msDays, setMsDays] = useState('14');

  const [ruleName, setRuleName] = useState('');
  const [triggerType, setTriggerType] = useState('TASK_STATUS_CHANGED');
  const [triggerValue, setTriggerValue] = useState('IN_REVIEW');
  const [actionType, setActionType] = useState('NOTIFY_LEAD');
  const [actionValue, setActionValue] = useState('');

  const timelineItems = useMemo(() => {
    const items = [];
    (milestones || []).forEach((m) =>
      items.push({
        id: m.id,
        kind: 'MILESTONE',
        title: m.title,
        status: m.status,
        dueDate: Number(m.dueDate || 0),
        raw: m,
      })
    );
    (tasks || []).forEach((t) =>
      items.push({
        id: t.id,
        kind: 'TASK',
        title: `${t.taskKey ? `[${t.taskKey}] ` : ''}${t.title}`,
        status: t.status,
        dueDate: Number(t.dueDate || 0),
        raw: t,
      })
    );
    return items.sort((a, b) => a.dueDate - b.dueDate);
  }, [tasks, milestones]);

  async function handleAddMilestone(e) {
    e.preventDefault();
    if (!msTitle.trim()) return;
    const days = parseInt(msDays, 10) || 14;
    await addDoc(collection(db, 'projects', project.id, 'milestones'), {
      projectId: project.id,
      title: msTitle.trim(),
      status: 'PENDING',
      dueDate: Date.now() + days * 86400000,
    });
    setMsTitle('');
  }

  async function handleAddRule(e) {
    e.preventDefault();
    const name = ruleName.trim() || `${triggerType} -> ${actionType}`;
    await addDoc(collection(db, 'projects', project.id, 'automations'), {
      projectId: project.id,
      name,
      triggerType,
      triggerValue: triggerValue.trim(),
      actionType,
      actionValue: actionValue.trim(),
      enabled: true,
    });
    setRuleName('');
    setTriggerValue('');
    setActionValue('');
  }

  return (
    <div className="grid-2">
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Calendar & Milestone Timeline</h3>
        <form onSubmit={handleAddMilestone} className="row" style={{ marginBottom: 12 }}>
          <input
            className="input"
            style={{ flex: 2 }}
            placeholder="New milestone title"
            value={msTitle}
            onChange={(e) => setMsTitle(e.target.value)}
          />
          <input
            className="input"
            style={{ width: 90 }}
            type="number"
            value={msDays}
            onChange={(e) => setMsDays(e.target.value)}
          />
          <button type="submit" className="btn btn-primary btn-sm">
            + Milestone
          </button>
        </form>

        {timelineItems.map((item) => (
          <div
            key={`${item.kind}-${item.id}`}
            className="row"
            style={{
              justifyContent: 'space-between',
              padding: '8px 0',
              borderBottom: '1px solid var(--border)',
            }}
          >
            <div>
              <span className="pill" style={{ marginRight: 8 }}>
                {item.kind}
              </span>
              <strong>{item.title}</strong>
              <div className="muted">
                {item.dueDate ? new Date(item.dueDate).toDateString() : 'No due date'} •{' '}
                {item.status}
              </div>
            </div>
            {item.kind === 'MILESTONE' && (
              <button
                type="button"
                className="btn btn-sm"
                onClick={() =>
                  updateDoc(doc(db, 'projects', project.id, 'milestones', item.id), {
                    status: item.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED',
                  })
                }
              >
                {item.status === 'COMPLETED' ? 'Reopen' : 'Complete'}
              </button>
            )}
          </div>
        ))}
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Workflow Automations</h3>
        <form onSubmit={handleAddRule} style={{ display: 'grid', gap: 8, marginBottom: 14 }}>
          <input
            className="input"
            placeholder="Rule name (e.g. Notify Lead on Review)"
            value={ruleName}
            onChange={(e) => setRuleName(e.target.value)}
          />
          <div className="row">
            <select
              className="select"
              style={{ flex: 1 }}
              value={triggerType}
              onChange={(e) => setTriggerType(e.target.value)}
            >
              <option value="TASK_ASSIGNED">TASK_ASSIGNED</option>
              <option value="TASK_STATUS_CHANGED">TASK_STATUS_CHANGED</option>
              <option value="ALL_SUBTASKS_COMPLETED">ALL_SUBTASKS_COMPLETED</option>
              <option value="MILESTONE_COMPLETED">MILESTONE_COMPLETED</option>
            </select>
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Match value (e.g. IN_REVIEW)"
              value={triggerValue}
              onChange={(e) => setTriggerValue(e.target.value)}
            />
          </div>
          <div className="row">
            <select
              className="select"
              style={{ flex: 1 }}
              value={actionType}
              onChange={(e) => setActionType(e.target.value)}
            >
              <option value="NOTIFY_ASSIGNEE">NOTIFY_ASSIGNEE</option>
              <option value="NOTIFY_LEAD">NOTIFY_LEAD</option>
              <option value="POST_CHAT_UPDATE">POST_CHAT_UPDATE</option>
              <option value="SET_TASK_STATUS">SET_TASK_STATUS</option>
              <option value="SET_TASK_PHASE">SET_TASK_PHASE</option>
            </select>
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Target value / message"
              value={actionValue}
              onChange={(e) => setActionValue(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary btn-sm">
            + Add Automation Rule
          </button>
        </form>

        {(automations || []).map((rule) => (
          <div
            key={rule.id}
            className="row"
            style={{
              justifyContent: 'space-between',
              padding: '8px 0',
              borderBottom: '1px solid var(--border)',
            }}
          >
            <div>
              <strong>{rule.name}</strong>
              <div className="muted">
                When {rule.triggerType} {rule.triggerValue || ''} {'->'} {rule.actionType}{' '}
                {rule.actionValue || ''}
              </div>
            </div>
            <div className="row">
              <button
                type="button"
                className="btn btn-sm"
                onClick={() =>
                  updateDoc(doc(db, 'projects', project.id, 'automations', rule.id), {
                    enabled: !rule.enabled,
                  })
                }
              >
                {rule.enabled ? 'Enabled' : 'Paused'}
              </button>
              <button
                type="button"
                className="btn btn-sm btn-danger"
                onClick={() =>
                  deleteDoc(doc(db, 'projects', project.id, 'automations', rule.id))
                }
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
