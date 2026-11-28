import React, { useMemo, useState } from 'react';
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  updateDoc,
} from 'firebase/firestore';
import { db } from '../firebase.js';

function statusPillClass(status) {
  const key = (status || '').toUpperCase();
  if (key === 'DONE') return 'pill pill-status-done';
  if (key === 'IN_REVIEW') return 'pill pill-status-review';
  if (key === 'IN_PROGRESS') return 'pill pill-status-progress';
  if (key === 'BLOCKED') return 'pill pill-status-blocked';
  if (key === 'TODO') return 'pill pill-status-todo';
  return 'pill';
}

export default function KanbanBoard({
  project,
  tasks,
  members,
  currentUser,
  onSelectTask,
  selectedTask,
}) {
  const [search, setSearch] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [onlyMine, setOnlyMine] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [issueType, setIssueType] = useState('TASK');
  const [phase, setPhase] = useState('DEVELOPMENT');
  const [dueDateStr, setDueDateStr] = useState('');
  const [dependsOnId, setDependsOnId] = useState('');
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');

  const statuses =
    project?.customStatuses && project.customStatuses.length > 0
      ? project.customStatuses
      : ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];

  const filteredTasks = useMemo(() => {
    const q = search.trim().toLowerCase();
    return (tasks || []).filter((t) => {
      if (onlyMine && t.assignedTo !== currentUser?.uid) return false;
      if (priorityFilter !== 'ALL' && (t.priority || 'MEDIUM') !== priorityFilter) {
        return false;
      }
      if (q) {
        const hay = `${t.taskKey || ''} ${t.title || ''} ${t.description || ''} ${t.assignedToName || ''}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [tasks, search, priorityFilter, onlyMine, currentUser]);

  async function recalculateProgress(nextTasks) {
    if (!nextTasks || nextTasks.length === 0) {
      await updateDoc(doc(db, 'projects', project.id), { progressPercentage: 0 });
      return;
    }
    const done = nextTasks.filter((t) => (t.status || '').toUpperCase() === 'DONE').length;
    const pct = Math.round((done / nextTasks.length) * 100);
    await updateDoc(doc(db, 'projects', project.id), { progressPercentage: pct });
  }

  async function handleCreateTask(e) {
    e.preventDefault();
    if (!title.trim()) return;
    const member = members.find((m) => m.userId === assigneeId);
    const blocker = tasks.find((t) => t.id === dependsOnId);
    const dueMs = dueDateStr ? new Date(dueDateStr).getTime() : Date.now() + 7 * 86400000;
    const taskRef = doc(collection(db, 'projects', project.id, 'tasks'));
    const taskKey = `TS-${Math.floor(100 + Math.random() * 900)}`;

    await addDoc(collection(db, 'projects', project.id, 'tasks'), {
      projectId: project.id,
      title: title.trim(),
      description: description.trim(),
      status: statuses[0] || 'TODO',
      priority,
      issueType,
      phase,
      taskKey,
      assignedTo: member ? member.userId : '',
      assignedToName: member ? member.name : '',
      dueDate: Number.isFinite(dueMs) ? dueMs : Date.now() + 7 * 86400000,
      dependsOnTaskId: blocker ? blocker.id : '',
      dependsOnTaskKey: blocker ? blocker.taskKey || blocker.title : '',
      subtasks: [],
      tags: [],
      commentsCount: 0,
      timeSpentHours: 0,
      startDate: Date.now(),
    });
    void taskRef;

    if (member && member.userId !== currentUser?.uid) {
      await addDoc(collection(db, 'users', member.userId, 'notifications'), {
        userId: member.userId,
        title: `Task Assigned: ${title.trim()}`,
        message: `You were assigned a task in ${project.title}`,
        type: 'TASK_ASSIGNED',
        projectId: project.id,
        projectTitle: project.title,
        timestamp: Date.now(),
        read: false,
      });
    }

    setTitle('');
    setDescription('');
    setAssigneeId('');
    setDependsOnId('');
  }

  async function handleStatusChange(task, newStatus) {
    await updateDoc(doc(db, 'projects', project.id, 'tasks', task.id), {
      status: newStatus,
    });
    const updated = tasks.map((t) => (t.id === task.id ? { ...t, status: newStatus } : t));
    await recalculateProgress(updated);
  }

  async function handleDeleteTask(task) {
    await deleteDoc(doc(db, 'projects', project.id, 'tasks', task.id));
    if (selectedTask?.id === task.id) onSelectTask(null);
    const remaining = tasks.filter((t) => t.id !== task.id);
    await recalculateProgress(remaining);
  }

  async function handleToggleSubtask(task, idx) {
    const next = (task.subtasks || []).map((s, i) =>
      i === idx ? { ...s, completed: !s.completed } : s
    );
    await updateDoc(doc(db, 'projects', project.id, 'tasks', task.id), {
      subtasks: next,
    });
  }

  async function handleAddSubtask(e, task) {
    e.preventDefault();
    if (!newSubtaskTitle.trim()) return;
    const next = [
      ...(task.subtasks || []),
      {
        subtaskId: `${Date.now()}`,
        title: newSubtaskTitle.trim(),
        completed: false,
      },
    ];
    await updateDoc(doc(db, 'projects', project.id, 'tasks', task.id), {
      subtasks: next,
    });
    setNewSubtaskTitle('');
  }

  return (
    <div>
      <div className="card" style={{ marginBottom: 16 }}>
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 10 }}>
          <strong>Create Task</strong>
          <div className="row">
            <input
              className="input"
              style={{ width: 220 }}
              placeholder="Search tasks..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="select"
              style={{ width: 130 }}
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
            >
              <option value="ALL">All Priorities</option>
              <option value="URGENT">Urgent</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
            <label className="row" style={{ fontSize: 13 }}>
              <input
                type="checkbox"
                checked={onlyMine}
                onChange={(e) => setOnlyMine(e.target.checked)}
              />
              My Tasks
            </label>
          </div>
        </div>

        <form onSubmit={handleCreateTask} className="grid-2">
          <input
            className="input"
            placeholder="Task title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
          <input
            className="input"
            placeholder="Description / acceptance criteria"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <select
            className="select"
            value={assigneeId}
            onChange={(e) => setAssigneeId(e.target.value)}
          >
            <option value="">Unassigned</option>
            {members.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.name} ({m.department || m.role})
              </option>
            ))}
          </select>
          <div className="row">
            <select
              className="select"
              style={{ flex: 1 }}
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="URGENT">URGENT</option>
            </select>
            <select
              className="select"
              style={{ flex: 1 }}
              value={issueType}
              onChange={(e) => setIssueType(e.target.value)}
            >
              <option value="TASK">TASK</option>
              <option value="FEATURE">FEATURE</option>
              <option value="BUG">BUG</option>
              <option value="DOCUMENTATION">DOC</option>
            </select>
            <select
              className="select"
              style={{ flex: 1 }}
              value={phase}
              onChange={(e) => setPhase(e.target.value)}
            >
              <option value="PLANNING">PLANNING</option>
              <option value="DESIGN">DESIGN</option>
              <option value="DEVELOPMENT">DEVELOPMENT</option>
              <option value="REVIEW">REVIEW</option>
            </select>
          </div>
          <div className="row">
            <input
              type="date"
              className="input"
              style={{ flex: 1 }}
              value={dueDateStr}
              onChange={(e) => setDueDateStr(e.target.value)}
            />
            <select
              className="select"
              style={{ flex: 1 }}
              value={dependsOnId}
              onChange={(e) => setDependsOnId(e.target.value)}
            >
              <option value="">No blocker</option>
              {tasks.map((t) => (
                <option key={t.id} value={t.id}>
                  Blocked by {t.taskKey || t.title}
                </option>
              ))}
            </select>
            <button type="submit" className="btn btn-primary">
              + Add Task
            </button>
          </div>
        </form>
      </div>

      <div className="kanban-board">
        {statuses.map((st) => {
          const colTasks = filteredTasks.filter(
            (t) => (t.status || 'TODO').toUpperCase() === st.toUpperCase()
          );
          return (
            <div className="kanban-col" key={st}>
              <div className="row" style={{ justifyContent: 'space-between', marginBottom: 10 }}>
                <span className={statusPillClass(st)}>{st.replace(/_/g, ' ')}</span>
                <span className={statusPillClass(st)}>{colTasks.length}</span>
              </div>

              {colTasks.map((task) => {
                const subTotal = (task.subtasks || []).length;
                const subDone = (task.subtasks || []).filter((s) => s.completed).length;
                return (
                  <div className="task-card" key={task.id}>
                    <div className="row" style={{ justifyContent: 'space-between' }}>
                      <span className="pill">
                        {task.taskKey || 'TS'} • {task.issueType || 'TASK'}
                      </span>
                      <span className="muted">{task.priority || 'MEDIUM'}</span>
                    </div>
                    <div
                      style={{ fontWeight: 700, marginTop: 6, cursor: 'pointer' }}
                      onClick={() => onSelectTask(task)}
                    >
                      {task.title}
                    </div>
                    {task.dependsOnTaskKey && (
                      <div style={{ marginTop: 6 }}>
                        <span className="pill pill-status-blocked">
                          Blocked by {task.dependsOnTaskKey}
                        </span>
                      </div>
                    )}
                    <div className="muted" style={{ marginTop: 6 }}>
                      Assignee: {task.assignedToName || 'Unassigned'}
                      {subTotal > 0 ? ` • Subtasks ${subDone}/${subTotal}` : ''}
                    </div>
                    <div className="row" style={{ marginTop: 8, justifyContent: 'space-between' }}>
                      <select
                        className="select"
                        style={{ width: 140, padding: '4px 6px' }}
                        value={task.status || 'TODO'}
                        onChange={(e) => handleStatusChange(task, e.target.value)}
                      >
                        {statuses.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={() => handleDeleteTask(task)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>

      {selectedTask && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <h3 style={{ margin: 0 }}>
              {selectedTask.taskKey}: {selectedTask.title}
            </h3>
            <button className="btn btn-sm" onClick={() => onSelectTask(null)}>
              Close
            </button>
          </div>
          <p className="muted">{selectedTask.description || 'No description.'}</p>

          <h4>Checklist / Subtasks</h4>
          {(selectedTask.subtasks || []).map((s, idx) => (
            <label key={s.subtaskId || idx} className="row" style={{ marginBottom: 6 }}>
              <input
                type="checkbox"
                checked={Boolean(s.completed)}
                onChange={() => handleToggleSubtask(selectedTask, idx)}
              />
              <span style={{ textDecoration: s.completed ? 'line-through' : 'none' }}>
                {s.title}
              </span>
            </label>
          ))}
          <form
            onSubmit={(e) => handleAddSubtask(e, selectedTask)}
            className="row"
            style={{ marginTop: 8 }}
          >
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Add checklist item..."
              value={newSubtaskTitle}
              onChange={(e) => setNewSubtaskTitle(e.target.value)}
            />
            <button type="submit" className="btn btn-primary btn-sm">
              + Add Item
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
