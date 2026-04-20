import { useState } from 'react';
import { editMessage, deleteMessage } from '../api/messages';
import styles from './MessageItem.module.css';

export function MessageItem({ message, currentUserId, isRoomAdmin, onReply, onUpdated }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(message.content);

  const isOwn = message.senderId === currentUserId;
  const canEdit = isOwn && !message.deleted;
  const canDelete = (isOwn || isRoomAdmin) && !message.deleted;

  async function handleEdit() {
    try {
      const updated = await editMessage(message.id, draft);
      onUpdated(updated);
      setEditing(false);
    } catch {
      // leave editing open so user can retry
    }
  }

  async function handleDelete() {
    try {
      await deleteMessage(message.id);
    } catch {
      // ignore — the broadcast will update the list
    }
  }

  return (
    <div className={`${styles.item} ${isOwn ? styles.own : ''}`}>
      <div className={styles.meta}>
        <span className={styles.sender}>{message.senderUsername}</span>
        <span className={styles.time}>{new Date(message.createdAt).toLocaleTimeString()}</span>
        {message.edited && !message.deleted && <span className={styles.edited}>(edited)</span>}
      </div>

      {message.replyToId && (
        <div className={styles.replyQuote}>↩ reply</div>
      )}

      {editing ? (
        <div className={styles.editBox}>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={2}
          />
          <div className={styles.editActions}>
            <button onClick={handleEdit}>Save</button>
            <button onClick={() => { setEditing(false); setDraft(message.content); }}>Cancel</button>
          </div>
        </div>
      ) : (
        <p className={`${styles.content} ${message.deleted ? styles.deleted : ''}`}>
          {message.content}
        </p>
      )}

      {!message.deleted && !editing && (
        <div className={styles.actions}>
          <button onClick={() => onReply(message)} title="Reply">↩</button>
          {canEdit && <button onClick={() => setEditing(true)} title="Edit">✏</button>}
          {canDelete && <button onClick={handleDelete} title="Delete">🗑</button>}
        </div>
      )}
    </div>
  );
}
