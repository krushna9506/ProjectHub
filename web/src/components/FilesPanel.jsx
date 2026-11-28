import React, { useState } from 'react';
import { addDoc, collection, deleteDoc, doc } from 'firebase/firestore';
import { deleteObject, getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { db, storage } from '../firebase.js';

export default function FilesPanel({ project, files, currentUser }) {
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [type, setType] = useState('LINK');
  const [accessLevel, setAccessLevel] = useState('MEMBERS_ONLY');
  const [language, setLanguage] = useState('JavaScript');
  const [snippet, setSnippet] = useState('');
  const [uploading, setUploading] = useState(false);

  const isLead = project?.leadId === currentUser?.uid;

  async function handleAddResource(e) {
    e.preventDefault();
    if (!name.trim()) return;
    let normalizedUrl = url.trim();
    if (type !== 'CODE' && normalizedUrl && !/^https?:\/\//i.test(normalizedUrl)) {
      normalizedUrl = `https://${normalizedUrl}`;
    }

    await addDoc(collection(db, 'projects', project.id, 'files'), {
      projectId: project.id,
      name: name.trim(),
      url: type === 'CODE' ? '' : normalizedUrl,
      type,
      snippetContent: type === 'CODE' ? snippet : '',
      language: type === 'CODE' ? language : 'General',
      uploadedById: currentUser.uid,
      uploadedByName: currentUser.displayName || currentUser.email,
      accessLevel,
      timestamp: Date.now(),
    });
    setName('');
    setUrl('');
    setSnippet('');
  }

  async function handleFileUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    try {
      const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
      const storagePath = `project_files/${project.id}/${Date.now()}_${safeName}`;
      const storageRef = ref(storage, storagePath);
      await uploadBytes(storageRef, file, {
        customMetadata: {
          uploadedBy: currentUser.uid,
          accessLevel,
        },
      });
      const downloadUrl = await getDownloadURL(storageRef);
      const isImage = /\.(png|jpe?g|webp|gif)$/i.test(file.name);
      await addDoc(collection(db, 'projects', project.id, 'files'), {
        projectId: project.id,
        name: file.name,
        url: downloadUrl,
        type: isImage ? 'IMAGE' : 'DOCUMENT',
        uploadedById: currentUser.uid,
        uploadedByName: currentUser.displayName || currentUser.email,
        storagePath,
        fileSizeBytes: file.size,
        accessLevel,
        timestamp: Date.now(),
      });
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(f) {
    if (f.storagePath) {
      try {
        await deleteObject(ref(storage, f.storagePath));
      } catch {
        // Ignore if storage object already deleted
      }
    }
    await deleteDoc(doc(db, 'projects', project.id, 'files', f.id));
  }

  return (
    <div className="grid-2">
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Share File, Code Snippet, or Link</h3>
        <form onSubmit={handleAddResource} style={{ display: 'grid', gap: 10 }}>
          <div className="row">
            <select className="select" value={type} onChange={(e) => setType(e.target.value)}>
              <option value="LINK">Resource Link</option>
              <option value="GITHUB">GitHub Repo</option>
              <option value="CODE">Code Snippet</option>
            </select>
            <select
              className="select"
              value={accessLevel}
              onChange={(e) => setAccessLevel(e.target.value)}
            >
              <option value="MEMBERS_ONLY">Team Members Only</option>
              <option value="LEAD_ONLY">Lead Only</option>
              <option value="PUBLIC">Public</option>
            </select>
          </div>

          <input
            className="input"
            placeholder="Title (e.g. API Contract / Figma)"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          {type === 'CODE' ? (
            <>
              <input
                className="input"
                placeholder="Language (e.g. Java, JavaScript)"
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
              />
              <textarea
                className="textarea"
                rows={4}
                placeholder="Paste code snippet..."
                value={snippet}
                onChange={(e) => setSnippet(e.target.value)}
              />
            </>
          ) : (
            <input
              className="input"
              placeholder="https://..."
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          )}

          <button type="submit" className="btn btn-primary">
            Share Resource
          </button>
        </form>

        <hr style={{ borderColor: 'var(--border)', margin: '16px 0' }} />
        <label className="btn" style={{ display: 'inline-block' }}>
          {uploading ? 'Uploading...' : 'Upload File to Storage'}
          <input type="file" style={{ display: 'none' }} onChange={handleFileUpload} />
        </label>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Shared Files & Snippets ({files.length})</h3>
        {files.map((f) => {
          const canDelete = isLead || f.uploadedById === currentUser.uid;
          return (
            <div
              key={f.id}
              style={{
                padding: '10px 0',
                borderBottom: '1px solid var(--border)',
              }}
            >
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong>{f.name}</strong>
                <span className="pill">
                  {f.type} • {f.accessLevel || 'MEMBERS_ONLY'}
                </span>
              </div>
              <div className="muted">By {f.uploadedByName || 'Member'}</div>
              {f.type === 'CODE' && f.snippetContent && (
                <pre className="code-block">
                  {f.snippetContent}
                </pre>
              )}
              <div className="row" style={{ marginTop: 6 }}>
                {f.url && (
                  <a className="btn btn-sm" href={f.url} target="_blank" rel="noreferrer">
                    Open
                  </a>
                )}
                {canDelete && (
                  <button
                    type="button"
                    className="btn btn-sm btn-danger"
                    onClick={() => handleDelete(f)}
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
