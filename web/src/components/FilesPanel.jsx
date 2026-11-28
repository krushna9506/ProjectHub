import React, { useState } from 'react';
import { collection, deleteDoc, doc, setDoc } from 'firebase/firestore';
import { deleteObject, getDownloadURL, ref, uploadBytes } from 'firebase/storage';
import { db, storage } from '../firebase.js';
import { isSafeHttpUrl, normalizeHttpUrl } from '../utils/safeUrl.js';

const MAX_UPLOAD_BYTES = 50 * 1024 * 1024;
const BLOCKED_UPLOAD_EXT_REGEX = /\.(html?|xhtml|svg|js|mjs|cjs|jsx|ts|tsx)$/i;
const BLOCKED_CONTENT_TYPE_REGEX = /(html|javascript|ecmascript|svg)/i;

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
    let normalizedUrl = '';
    if (type !== 'CODE') {
      if (url.trim()) {
        normalizedUrl = normalizeHttpUrl(url);
        if (!normalizedUrl) {
          alert('Please enter a valid http:// or https:// URL.');
          return;
        }
      } else {
        alert('Please provide a URL for this resource.');
        return;
      }
    }

    try {
      const fileDocRef = doc(collection(db, 'projects', project.id, 'files'));
      await setDoc(fileDocRef, {
        fileId: fileDocRef.id,
        projectId: project.id,
        name: name.trim(),
        url: normalizedUrl,
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
    } catch (err) {
      alert(`Failed to share resource: ${err.message}`);
    }
  }

  async function handleFileUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size >= MAX_UPLOAD_BYTES) {
      alert('File exceeds the 50MB upload limit.');
      e.target.value = '';
      return;
    }
    if (BLOCKED_UPLOAD_EXT_REGEX.test(file.name) || BLOCKED_CONTENT_TYPE_REGEX.test(file.type || '')) {
      alert('Uploading active HTML/JavaScript/SVG files is blocked for security.');
      e.target.value = '';
      return;
    }
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
      const fileDocRef = doc(collection(db, 'projects', project.id, 'files'));
      await setDoc(fileDocRef, {
        fileId: fileDocRef.id,
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
    } catch (err) {
      alert(`Upload failed: ${err.message}`);
    } finally {
      e.target.value = '';
      setUploading(false);
    }
  }

  async function handleDelete(f) {
    if (!f?.id) return;
    try {
      if (f.storagePath) {
        try {
          await deleteObject(ref(storage, f.storagePath));
        } catch {
          // Ignore if storage object already deleted
        }
      }
      await deleteDoc(doc(db, 'projects', project.id, 'files', f.id));
    } catch (err) {
      alert(`Failed to delete file: ${err.message}`);
    }
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
                {isSafeHttpUrl(f.url) && (
                  <a className="btn btn-sm" href={f.url} target="_blank" rel="noopener noreferrer">
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
