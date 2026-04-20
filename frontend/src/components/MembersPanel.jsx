import { useEffect, useState } from 'react';
import { listMembers } from '../api/rooms';
import { PresenceIndicator } from './PresenceIndicator';
import styles from './MembersPanel.module.css';

export function MembersPanel({ roomId, currentUserId, onManage, isAdminOrOwner }) {
  const [members, setMembers] = useState([]);

  useEffect(() => {
    if (!roomId) return;
    listMembers(roomId).then(setMembers).catch(() => {});
  }, [roomId]);

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <h3>Members ({members.length})</h3>
        {isAdminOrOwner && (
          <button onClick={onManage}>Manage</button>
        )}
      </div>
      <ul className={styles.list}>
        {members.map(m => (
          <li key={m.userId} className={styles.member}>
            <PresenceIndicator state="ONLINE" />
            <span className={styles.username}>{m.username}</span>
            <span className={styles.role}>{m.role}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
