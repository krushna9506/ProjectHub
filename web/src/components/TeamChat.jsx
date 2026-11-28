import React, { useState } from 'react';
import { collection, doc, runTransaction, setDoc } from 'firebase/firestore';
import { db } from '../firebase.js';

const REACTION_EMOJIS = ['👍', '❤️', '🔥', '✅', '🎉'];

export default function TeamChat({ project, messages, members, currentUser }) {
  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState(null);

  function insertMention(member) {
    const handle = `@${(member.name || member.email.split('@')[0]).replace(/\s+/g, '')}`;
    setText((prev) => (prev ? `${prev} ${handle} ` : `${handle} `));
  }

  async function handleSend(e) {
    e.preventDefault();
    if (!text.trim()) return;

    const mentionedIds = [];
    const tokens = (text.match(/@([A-Za-z0-9._-]+)/g) || []).map((t) =>
      t.slice(1).toLowerCase()
    );
    members.forEach((m) => {
      const compact = (m.name || '').replace(/\s+/g, '').toLowerCase();
      const first = (m.name || '').split(/\s+/)[0].toLowerCase();
      if (tokens.includes(compact) || tokens.includes(first)) {
        mentionedIds.push(m.userId);
      }
    });

    const senderName =
      currentUser?.displayName || currentUser?.email?.split('@')[0] || 'Teammate';

    try {
      const msgRef = doc(collection(db, 'projects', project.id, 'messages'));
      await setDoc(msgRef, {
        messageId: msgRef.id,
        projectId: project.id,
        senderId: currentUser.uid,
        senderName,
        senderEmail: currentUser.email || '',
        message: text.trim(),
        messageType: 'TEXT',
        timestamp: Date.now(),
        replyToMessageId: replyTo ? replyTo.id : '',
        replyToSenderName: replyTo ? replyTo.senderName : '',
        replyToPreview: replyTo ? replyTo.message : '',
        reactions: {},
        mentionedUserIds: mentionedIds,
      });

      for (const uid of mentionedIds) {
        if (uid !== currentUser.uid) {
          const notifRef = doc(collection(db, 'users', uid, 'notifications'));
          await setDoc(notifRef, {
            notificationId: notifRef.id,
            userId: uid,
            title: `${senderName} mentioned you in chat`,
            message: text.trim(),
            type: 'MENTION',
            projectId: project.id,
            projectTitle: project.title,
            timestamp: Date.now(),
            read: false,
          });
        }
      }

      setText('');
      setReplyTo(null);
    } catch (err) {
      alert(`Failed to send message: ${err.message}`);
    }
  }

  async function handleToggleReaction(msgId, emoji) {
    const msgRef = doc(db, 'projects', project.id, 'messages', msgId);
    await runTransaction(db, async (tx) => {
      const snap = await tx.get(msgRef);
      if (!snap.exists()) return;
      const data = snap.data();
      const reactions = { ...(data.reactions || {}) };
      const users = [...(reactions[emoji] || [])];
      const idx = users.indexOf(currentUser.uid);
      if (idx >= 0) users.splice(idx, 1);
      else users.push(currentUser.uid);
      if (users.length === 0) delete reactions[emoji];
      else reactions[emoji] = users;
      tx.update(msgRef, { reactions });
    });
  }

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <strong>Team Chat</strong>
        <a
          className="btn btn-sm"
          href={`https://meet.jit.si/ProjectHub_${(project.id || '').replace(/[^a-zA-Z0-9]/g, '')}`}
          target="_blank"
          rel="noreferrer"
        >
          Join Live Jitsi Call
        </a>
      </div>

      <div style={{ maxHeight: 420, overflowY: 'auto', marginBottom: 12 }}>
        {(messages || []).map((m) => (
          <div
            key={m.id}
            style={{
              borderBottom: '1px solid var(--border)',
              padding: '10px 0',
            }}
          >
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <strong>{m.senderName || 'Teammate'}</strong>
              <span className="muted">
                {m.timestamp ? new Date(m.timestamp).toLocaleString() : ''}
              </span>
            </div>
            {m.replyToPreview && (
              <div
                className="muted reply-banner"
                style={{
                  marginTop: 4,
                }}
              >
                Reply to {m.replyToSenderName}: {m.replyToPreview}
              </div>
            )}
            <div style={{ marginTop: 4 }}>{m.message}</div>
            <div className="row" style={{ marginTop: 6 }}>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setReplyTo(m)}
              >
                Reply
              </button>
              {REACTION_EMOJIS.map((emoji) => {
                const count = (m.reactions?.[emoji] || []).length;
                return (
                  <button
                    key={emoji}
                    type="button"
                    className="btn btn-sm"
                    onClick={() => handleToggleReaction(m.id, emoji)}
                  >
                    {emoji} {count > 0 ? count : ''}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {replyTo && (
        <div
          className="row reply-banner"
          style={{
            justifyContent: 'space-between',
            marginBottom: 8,
          }}
        >
          <span className="muted">
            Replying to {replyTo.senderName}: {replyTo.message}
          </span>
          <button type="button" className="btn btn-sm" onClick={() => setReplyTo(null)}>
            Cancel
          </button>
        </div>
      )}

      <div className="row" style={{ marginBottom: 8 }}>
        <span className="muted">Mention:</span>
        {members.map((m) => (
          <button
            key={m.userId}
            type="button"
            className="btn btn-sm"
            onClick={() => insertMention(m)}
          >
            @{m.name?.split(' ')[0] || 'member'}
          </button>
        ))}
      </div>

      <form onSubmit={handleSend} className="row">
        <input
          className="input"
          style={{ flex: 1 }}
          placeholder="Message team (use @name to mention)..."
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button type="submit" className="btn btn-primary">
          Send
        </button>
      </form>
    </div>
  );
}
