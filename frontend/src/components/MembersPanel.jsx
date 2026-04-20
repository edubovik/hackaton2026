import { useEffect, useState } from 'react';
import { listMembers } from '../api/rooms';
import { sendFriendRequest } from '../api/contacts';
import { PresenceIndicator } from './PresenceIndicator';
import styles from './MembersPanel.module.css';

export function MembersPanel({ roomId, currentUserId, isAdminOrOwner, presenceMap = {} }) {
  const [members, setMembers] = useState([]);
  const [requested, setRequested] = useState(new Set());

  useEffect(() => {
    if (!roomId) return;
    listMembers(roomId).then(setMembers).catch(() => {});
    setRequested(new Set());
  }, [roomId]);

  async function handleAddFriend(username) {
    try {
      await sendFriendRequest(username);
      setRequested(prev => new Set(prev).add(username));
    } catch {
      // already friends or request pending — treat as success visually
      setRequested(prev => new Set(prev).add(username));
    }
  }

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <h3>Members ({members.length})</h3>
      </div>
      <ul className={styles.list}>
        {members.map(m => (
          <li key={m.userId} className={styles.member}>
            <PresenceIndicator state={presenceMap[m.userId] ?? 'OFFLINE'} />
            <div className={styles.info}>
              <span className={styles.username}>{m.username}</span>
              <span className={styles.role}>{m.role}</span>
            </div>
            {m.userId !== currentUserId && (
              <button
                className={styles.addFriendBtn}
                onClick={() => handleAddFriend(m.username)}
                disabled={requested.has(m.username)}
                title={requested.has(m.username) ? 'Request sent' : 'Add friend'}
              >
                {requested.has(m.username) ? '✓' : '+'}
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
