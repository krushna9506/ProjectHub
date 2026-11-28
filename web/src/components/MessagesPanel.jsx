import React, { useEffect, useMemo, useState } from 'react';
import {
  arrayRemove,
  collection,
  doc,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  setDoc,
  updateDoc,
} from 'firebase/firestore';
import { db } from '../firebase.js';

const REACTION_EMOJIS = ['👍', '❤️', '🔥', '✅', '🎉'];

export function isConversationUnread(conv, currentUid) {
  if (!conv || !currentUid) return false;
  const lastTs = conv.lastMessageTimestamp || 0;
  if (lastTs <= 0) return false;
  if (conv.lastMessageSenderId === currentUid) return false;
  const readAt = conv.readBy?.[currentUid] || 0;
  return readAt < lastTs;
}

export function resolveConversationTitle(conv, currentUid) {
  if (!conv) return 'Conversation';
  if (conv.type === 'GROUP') {
    return (conv.title || '').trim() || 'Group Chat';
  }
  const otherUid = (conv.participantIds || []).find((uid) => uid !== currentUid) || '';
  return (
    conv.participantNames?.[otherUid] ||
    conv.participantEmails?.[otherUid] ||
    'Direct Message'
  );
}

export default function MessagesPanel({
  conversations,
  selectedConversationId,
  onSelectConversation,
  campusUsers,
  currentUser,
  onOpenDirectChat,
}) {
  const [filterType, setFilterType] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showDmPicker, setShowDmPicker] = useState(false);
  const [showGroupForm, setShowGroupForm] = useState(false);

  const [groupTitle, setGroupTitle] = useState('');
  const [groupDesc, setGroupDesc] = useState('');
  const [selectedGroupMemberIds, setSelectedGroupMemberIds] = useState([]);

  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [addMemberUid, setAddMemberUid] = useState('');

  const myUid = currentUser?.uid || '';
  const myName =
    currentUser?.displayName || currentUser?.email?.split('@')[0] || 'Member';

  const selectedConversation = useMemo(
    () => (conversations || []).find((c) => c.id === selectedConversationId) || null,
    [conversations, selectedConversationId]
  );

  const filteredConversations = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return (conversations || []).filter((c) => {
      if (filterType === 'DIRECT' && c.type === 'GROUP') return false;
      if (filterType === 'GROUP' && c.type !== 'GROUP') return false;
      if (!q) return true;
      const title = resolveConversationTitle(c, myUid).toLowerCase();
      const desc = (c.description || '').toLowerCase();
      const preview = (c.lastMessage || '').toLowerCase();
      return title.includes(q) || desc.includes(q) || preview.includes(q);
    });
  }, [conversations, filterType, searchQuery, myUid]);

  const otherCampusUsers = useMemo(
    () => (campusUsers || []).filter((u) => u.userId && u.userId !== myUid),
    [campusUsers, myUid]
  );

  useEffect(() => {
    if (!selectedConversationId || !db) {
      setMessages([]);
      return;
    }
    const unsub = onSnapshot(
      query(
        collection(db, 'conversations', selectedConversationId, 'messages'),
        orderBy('timestamp', 'asc')
      ),
      (snap) => {
        setMessages(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
      }
    );
    return () => unsub();
  }, [selectedConversationId]);

  useEffect(() => {
    if (!selectedConversation || !myUid || !db) return;
    if (isConversationUnread(selectedConversation, myUid)) {
      updateDoc(doc(db, 'conversations', selectedConversation.id), {
        [`readBy.${myUid}`]: Date.now(),
      }).catch(() => {});
    }
  }, [selectedConversation?.id, selectedConversation?.lastMessageTimestamp, myUid]);

  function toggleGroupMember(uid) {
    setSelectedGroupMemberIds((prev) =>
      prev.includes(uid) ? prev.filter((id) => id !== uid) : [...prev, uid]
    );
  }

  async function handleCreateGroup(e) {
    e.preventDefault();
    const cleanTitle = groupTitle.trim();
    if (!cleanTitle) return;

    const convRef = doc(collection(db, 'conversations'));
    const now = Date.now();
    const participantIds = [myUid, ...selectedGroupMemberIds.filter((id) => id !== myUid)];
    const participantNames = { [myUid]: myName };
    const participantEmails = { [myUid]: currentUser?.email || '' };

    otherCampusUsers.forEach((u) => {
      if (participantIds.includes(u.userId)) {
        participantNames[u.userId] =
          u.name || (u.email ? u.email.split('@')[0] : 'Member');
        participantEmails[u.userId] = u.email || '';
      }
    });

    const systemText = `${myName} created group "${cleanTitle}"`;
    await setDoc(convRef, {
      conversationId: convRef.id,
      type: 'GROUP',
      title: cleanTitle,
      description: groupDesc.trim(),
      createdBy: myUid,
      participantIds,
      participantNames,
      participantEmails,
      lastMessage: systemText.slice(0, 480),
      lastMessageSenderId: myUid,
      lastMessageSenderName: myName,
      lastMessageTimestamp: now,
      createdAt: now,
      readBy: { [myUid]: now },
    });

    const msgRef = doc(collection(db, 'conversations', convRef.id, 'messages'));
    await setDoc(msgRef, {
      messageId: msgRef.id,
      projectId: convRef.id,
      senderId: myUid,
      senderName: myName,
      senderEmail: currentUser?.email || '',
      message: systemText,
      messageType: 'SYSTEM',
      timestamp: now,
      replyToMessageId: '',
      replyToSenderName: '',
      replyToPreview: '',
      reactions: {},
      mentionedUserIds: [],
    });

    for (const uid of participantIds) {
      if (uid !== myUid) {
        const notifRef = doc(collection(db, 'users', uid, 'notifications'));
        await setDoc(notifRef, {
          notificationId: notifRef.id,
          userId: uid,
          title: `Added to ${cleanTitle}`,
          message: `${myName} created a group chat with you`,
          type: 'GROUP_CHAT',
          projectId: convRef.id,
          projectTitle: cleanTitle,
          timestamp: now,
          read: false,
        });
      }
    }

    setGroupTitle('');
    setGroupDesc('');
    setSelectedGroupMemberIds([]);
    setShowGroupForm(false);
    onSelectConversation(convRef.id);
  }

  function resolveGroupParticipants() {
    if (!selectedConversation) return [];
    return (selectedConversation.participantIds || []).map((uid) => {
      const found = campusUsers.find((u) => u.userId === uid);
      if (found) return found;
      return {
        userId: uid,
        name: selectedConversation.participantNames?.[uid] || 'Member',
        email: selectedConversation.participantEmails?.[uid] || '',
      };
    });
  }

  function insertMention(member) {
    const base = member.name || member.email?.split('@')[0] || 'member';
    const handle = `@${base.replace(/\s+/g, '')}`;
    setText((prev) => (prev ? `${prev} ${handle} ` : `${handle} `));
  }

  async function handleSendMessage(e) {
    e.preventDefault();
    if (!selectedConversation || !text.trim()) return;
    const body = text.trim();
    const now = Date.now();
    const convId = selectedConversation.id;
    const isGroup = selectedConversation.type === 'GROUP';

    const mentionedIds = [];
    if (isGroup) {
      const tokens = (body.match(/@([A-Za-z0-9._-]+)/g) || []).map((t) =>
        t.slice(1).toLowerCase()
      );
      resolveGroupParticipants().forEach((m) => {
        const compact = (m.name || '').replace(/\s+/g, '').toLowerCase();
        const first = (m.name || '').split(/\s+/)[0].toLowerCase();
        if (tokens.includes(compact) || tokens.includes(first)) {
          mentionedIds.push(m.userId);
        }
      });
    }

    const msgRef = doc(collection(db, 'conversations', convId, 'messages'));
    try {
      await setDoc(msgRef, {
        messageId: msgRef.id,
        projectId: convId,
        senderId: myUid,
        senderName: myName,
        senderEmail: currentUser?.email || '',
        message: body,
        messageType: 'TEXT',
        timestamp: now,
        replyToMessageId: replyTo ? replyTo.id : '',
        replyToSenderName: replyTo ? replyTo.senderName : '',
        replyToPreview: replyTo ? replyTo.message : '',
        reactions: {},
        mentionedUserIds: mentionedIds,
      });

      await updateDoc(doc(db, 'conversations', convId), {
        lastMessage: body.slice(0, 480),
        lastMessageSenderId: myUid,
        lastMessageSenderName: myName,
        lastMessageTimestamp: now,
        [`readBy.${myUid}`]: now,
      });

      const convTitle = isGroup ? selectedConversation.title : myName;
      const notified = new Set();

      for (const uid of mentionedIds) {
        if (uid && uid !== myUid) {
          notified.add(uid);
          const notifRef = doc(collection(db, 'users', uid, 'notifications'));
          await setDoc(notifRef, {
            notificationId: notifRef.id,
            userId: uid,
            title: `${myName} mentioned you in ${convTitle}`,
            message: body,
            type: 'MENTION',
            projectId: convId,
            projectTitle: convTitle,
            timestamp: now,
            read: false,
          });
        }
      }

      const notifType = isGroup ? 'GROUP_CHAT' : 'DM';
      const notifTitle = isGroup
        ? `${myName} in ${convTitle}`
        : `Message from ${myName}`;

      for (const uid of selectedConversation.participantIds || []) {
        if (uid && uid !== myUid && !notified.has(uid)) {
          const notifRef = doc(collection(db, 'users', uid, 'notifications'));
          await setDoc(notifRef, {
            notificationId: notifRef.id,
            userId: uid,
            title: notifTitle,
            message: body,
            type: notifType,
            projectId: convId,
            projectTitle: convTitle,
            timestamp: now,
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
    if (!selectedConversation) return;
    const msgRef = doc(db, 'conversations', selectedConversation.id, 'messages', msgId);
    await runTransaction(db, async (tx) => {
      const snap = await tx.get(msgRef);
      if (!snap.exists()) return;
      const data = snap.data();
      const reactions = { ...(data.reactions || {}) };
      const users = [...(reactions[emoji] || [])];
      const idx = users.indexOf(myUid);
      if (idx >= 0) users.splice(idx, 1);
      else users.push(myUid);
      if (users.length === 0) delete reactions[emoji];
      else reactions[emoji] = users;
      tx.update(msgRef, { reactions });
    });
  }

  async function handleAddMember() {
    if (!selectedConversation || selectedConversation.type !== 'GROUP' || !addMemberUid) return;
    const userObj = otherCampusUsers.find((u) => u.userId === addMemberUid);
    if (!userObj) return;
    const ids = Array.from(
      new Set([...(selectedConversation.participantIds || []), addMemberUid])
    );
    const names = {
      ...(selectedConversation.participantNames || {}),
      [addMemberUid]:
        userObj.name || (userObj.email ? userObj.email.split('@')[0] : 'Member'),
    };
    const emails = {
      ...(selectedConversation.participantEmails || {}),
      [addMemberUid]: userObj.email || '',
    };

    try {
      await updateDoc(doc(db, 'conversations', selectedConversation.id), {
        participantIds: ids,
        participantNames: names,
        participantEmails: emails,
      });
      const notifRef = doc(collection(db, 'users', addMemberUid, 'notifications'));
      await setDoc(notifRef, {
        notificationId: notifRef.id,
        userId: addMemberUid,
        title: `Added to ${selectedConversation.title}`,
        message: `${myName} added you to the group chat`,
        type: 'GROUP_CHAT',
        projectId: selectedConversation.id,
        projectTitle: selectedConversation.title,
        timestamp: Date.now(),
        read: false,
      });
      setAddMemberUid('');
    } catch (err) {
      alert(`Failed to add member: ${err.message}`);
    }
  }

  async function handleLeaveGroup() {
    if (!selectedConversation || selectedConversation.type !== 'GROUP') return;
    if ((selectedConversation.participantIds || []).length <= 1) {
      alert('You are the only member left in this group.');
      return;
    }
    if (!window.confirm(`Leave "${selectedConversation.title}"?`)) return;
    await updateDoc(doc(db, 'conversations', selectedConversation.id), {
      participantIds: arrayRemove(myUid),
    });
    onSelectConversation('');
  }

  const addableUsers = selectedConversation?.type === 'GROUP'
    ? otherCampusUsers.filter(
        (u) => !(selectedConversation.participantIds || []).includes(u.userId)
      )
    : [];

  return (
    <div className="messages-layout">
      <aside className="card messages-sidebar">
        <div className="row" style={{ justifyContent: 'space-between', marginBottom: 10 }}>
          <strong style={{ fontSize: 16 }}>Messages</strong>
          <div className="row">
            <button
              type="button"
              className="btn btn-sm btn-primary"
              onClick={() => {
                setShowDmPicker((prev) => !prev);
                setShowGroupForm(false);
              }}
            >
              + 1:1 DM
            </button>
            <button
              type="button"
              className="btn btn-sm btn-accent"
              onClick={() => {
                setShowGroupForm((prev) => !prev);
                setShowDmPicker(false);
              }}
            >
              + Group
            </button>
          </div>
        </div>

        {showDmPicker && (
          <div className="task-card" style={{ marginBottom: 10 }}>
            <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
              <strong style={{ fontSize: 13 }}>Start Direct Chat</strong>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setShowDmPicker(false)}
              >
                Close
              </button>
            </div>
            <div style={{ maxHeight: 170, overflowY: 'auto', display: 'grid', gap: 6 }}>
              {otherCampusUsers.map((u) => (
                <button
                  key={u.userId}
                  type="button"
                  className="btn btn-sm"
                  style={{ justifyContent: 'space-between' }}
                  onClick={() => {
                    onOpenDirectChat(u);
                    setShowDmPicker(false);
                  }}
                >
                  <span>{u.name || u.email}</span>
                  <span className="muted">{u.department || u.role || 'STUDENT'}</span>
                </button>
              ))}
              {otherCampusUsers.length === 0 && (
                <span className="muted">No other users loaded yet.</span>
              )}
            </div>
          </div>
        )}

        {showGroupForm && (
          <form onSubmit={handleCreateGroup} className="task-card" style={{ display: 'grid', gap: 8 }}>
            <strong style={{ fontSize: 13 }}>Create Group Chat</strong>
            <input
              className="input"
              placeholder="Group name (e.g. Capstone Backend)"
              value={groupTitle}
              onChange={(e) => setGroupTitle(e.target.value)}
              required
            />
            <input
              className="input"
              placeholder="Topic / purpose (optional)"
              value={groupDesc}
              onChange={(e) => setGroupDesc(e.target.value)}
            />
            <div className="muted">Add members:</div>
            <div style={{ maxHeight: 140, overflowY: 'auto', display: 'grid', gap: 4 }}>
              {otherCampusUsers.map((u) => (
                <label key={u.userId} className="row" style={{ fontSize: 12.5, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    checked={selectedGroupMemberIds.includes(u.userId)}
                    onChange={() => toggleGroupMember(u.userId)}
                  />
                  <span>{u.name || u.email}</span>
                </label>
              ))}
            </div>
            <div className="row">
              <button type="submit" className="btn btn-sm btn-primary">
                Create Group
              </button>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setShowGroupForm(false)}
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        <input
          className="input"
          style={{ marginBottom: 8 }}
          placeholder="Search messages or people..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />

        <div className="tabs" style={{ margin: '0 0 10px', width: '100%' }}>
          {[
            ['ALL', 'All'],
            ['DIRECT', '1:1'],
            ['GROUP', 'Groups'],
          ].map(([key, label]) => (
            <button
              key={key}
              type="button"
              className={`tab-btn ${filterType === key ? 'active' : ''}`}
              style={{ flex: 1, padding: '6px 10px', fontSize: 12 }}
              onClick={() => setFilterType(key)}
            >
              {label}
            </button>
          ))}
        </div>

        <div style={{ overflowY: 'auto', maxHeight: 460 }}>
          {filteredConversations.length === 0 && (
            <p className="muted">No conversations match your filter.</p>
          )}
          {filteredConversations.map((c) => {
            const title = resolveConversationTitle(c, myUid);
            const unread = isConversationUnread(c, myUid);
            const active = c.id === selectedConversationId;
            return (
              <div
                key={c.id}
                className={`conv-item ${active ? 'active' : ''}`}
                onClick={() => onSelectConversation(c.id)}
              >
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong style={{ fontSize: 13.5 }}>{title}</strong>
                  <div className="row">
                    <span className={`pill ${c.type === 'GROUP' ? 'pill-accent' : ''}`}>
                      {c.type === 'GROUP' ? `GROUP • ${(c.participantIds || []).length}` : '1:1'}
                    </span>
                    {unread && <span className="unread-dot" aria-label="Unread messages" />}
                  </div>
                </div>
                <div className="muted" style={{ marginTop: 4, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {c.lastMessage
                    ? c.lastMessageSenderId === myUid && c.type !== 'GROUP'
                      ? `You: ${c.lastMessage}`
                      : c.lastMessage
                    : c.type === 'GROUP'
                      ? c.description || 'Group channel'
                      : 'Tap to start chatting'}
                </div>
              </div>
            );
          })}
        </div>
      </aside>

      <section className="card messages-pane">
        {!selectedConversation ? (
          <div style={{ margin: 'auto', textAlign: 'center', padding: 32 }}>
            <h3 style={{ marginTop: 0 }}>Select a conversation</h3>
            <p className="muted">
              Choose a 1:1 direct thread or Group Chat on the left, or start a new one above.
            </p>
          </div>
        ) : (
          <>
            <div
              className="row"
              style={{
                justifyContent: 'space-between',
                paddingBottom: 12,
                borderBottom: '1px solid var(--border)',
              }}
            >
              <div>
                <div className="row">
                  <strong style={{ fontSize: 16 }}>
                    {resolveConversationTitle(selectedConversation, myUid)}
                  </strong>
                  <span className={`pill ${selectedConversation.type === 'GROUP' ? 'pill-accent' : ''}`}>
                    {selectedConversation.type === 'GROUP'
                      ? `GROUP • ${(selectedConversation.participantIds || []).length} members`
                      : '1:1 Direct Message'}
                  </span>
                </div>
                {selectedConversation.description && (
                  <div className="muted">{selectedConversation.description}</div>
                )}
              </div>

              {selectedConversation.type === 'GROUP' && (
                <div className="row">
                  {addableUsers.length > 0 && (
                    <>
                      <select
                        className="select"
                        style={{ width: 160, padding: '5px 8px' }}
                        value={addMemberUid}
                        onChange={(e) => setAddMemberUid(e.target.value)}
                      >
                        <option value="">Add member...</option>
                        {addableUsers.map((u) => (
                          <option key={u.userId} value={u.userId}>
                            {u.name || u.email}
                          </option>
                        ))}
                      </select>
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={handleAddMember}
                      >
                        Add
                      </button>
                    </>
                  )}
                  {(selectedConversation.participantIds || []).length > 1 && (
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      onClick={handleLeaveGroup}
                    >
                      Leave
                    </button>
                  )}
                </div>
              )}
            </div>

            <div style={{ flex: 1, overflowY: 'auto', padding: '12px 0', minHeight: 340, maxHeight: 460 }}>
              {messages.length === 0 && (
                <p className="muted">No messages yet. Say hello!</p>
              )}
              {messages.map((m) => {
                const isSent = m.senderId === myUid;
                return (
                  <div
                    key={m.id}
                    className={`chat-bubble-row ${isSent ? 'sent' : 'received'}`}
                  >
                    <div className={isSent ? 'chat-bubble-sent' : 'chat-bubble-received'}>
                      {!isSent && (
                        <div style={{ fontSize: 11, fontWeight: 700, opacity: 0.8, marginBottom: 2 }}>
                          {m.senderName || 'Member'}
                        </div>
                      )}
                      {m.replyToPreview && (
                        <div className="reply-banner" style={{ marginBottom: 6, fontSize: 11.5 }}>
                          Reply to {m.replyToSenderName}: {m.replyToPreview}
                        </div>
                      )}
                      <div>{m.message}</div>
                      <div className="row" style={{ marginTop: 6, justifyContent: 'space-between' }}>
                        <span style={{ fontSize: 10.5, opacity: 0.75 }}>
                          {m.timestamp ? new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                        <div className="row">
                          <button
                            type="button"
                            className="btn btn-sm"
                            style={{ minHeight: 24, padding: '2px 7px', fontSize: 11 }}
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
                                style={{ minHeight: 24, padding: '2px 6px', fontSize: 11 }}
                                onClick={() => handleToggleReaction(m.id, emoji)}
                              >
                                {emoji} {count > 0 ? count : ''}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {replyTo && (
              <div
                className="row reply-banner"
                style={{ justifyContent: 'space-between', marginBottom: 8 }}
              >
                <span>
                  Replying to {replyTo.senderName}: {replyTo.message}
                </span>
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => setReplyTo(null)}
                >
                  Cancel
                </button>
              </div>
            )}

            {selectedConversation.type === 'GROUP' && (
              <div className="row" style={{ marginBottom: 8 }}>
                <span className="muted">Mention:</span>
                {resolveGroupParticipants()
                  .filter((m) => m.userId !== myUid)
                  .map((m) => (
                    <button
                      key={m.userId}
                      type="button"
                      className="btn btn-sm"
                      onClick={() => insertMention(m)}
                    >
                      @{(m.name || m.email || 'member').split(/\s+/)[0]}
                    </button>
                  ))}
              </div>
            )}

            <form onSubmit={handleSendMessage} className="row">
              <input
                className="input"
                style={{ flex: 1 }}
                placeholder={
                  selectedConversation.type === 'GROUP'
                    ? 'Message group (use @name to mention)...'
                    : 'Write a direct message...'
                }
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
              <button type="submit" className="btn btn-primary">
                Send
              </button>
            </form>
          </>
        )}
      </section>
    </div>
  );
}
