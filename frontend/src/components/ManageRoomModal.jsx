import { useEffect, useState } from 'react';
import {
  listMembers,
  listBans,
  banMember,
  unbanMember,
  promoteAdmin,
  demoteAdmin,
  inviteUser,
  updateRoom,
  deleteRoom,
} from '../api/rooms';
import styles from './ManageRoomModal.module.css';

const TABS = ['Members', 'Admins', 'Banned', 'Invitations', 'Settings'];

export function ManageRoomModal({ room, currentUserId, isOwner, onClose, onDeleted }) {
  const [tab, setTab] = useState('Members');
  const [members, setMembers] = useState([]);
  const [bans, setBans] = useState([]);
  const [inviteUsername, setInviteUsername] = useState('');
  const [settingsName, setSettingsName] = useState(room.name);
  const [settingsDesc, setSettingsDesc] = useState(room.description || '');
  const [settingsPublic, setSettingsPublic] = useState(room.isPublic);
  const [error, setError] = useState('');

  async function loadData() {
    const [m, b] = await Promise.all([listMembers(room.id), listBans(room.id)]);
    setMembers(m);
    setBans(b);
  }

  useEffect(() => { loadData(); }, [room.id]);

  async function handleBan(userId, username) {
    if (!window.confirm(`Ban "${username}" from this room?`)) return;
    try { await banMember(room.id, userId); await loadData(); } catch (err) { setError(err.message); }
  }

  async function handleUnban(userId, username) {
    if (!window.confirm(`Unban "${username}"?`)) return;
    try { await unbanMember(room.id, userId); await loadData(); } catch (err) { setError(err.message); }
  }

  async function handlePromote(userId, username) {
    if (!window.confirm(`Make "${username}" an admin?`)) return;
    try { await promoteAdmin(room.id, userId); await loadData(); } catch (err) { setError(err.message); }
  }

  async function handleDemote(userId, username) {
    if (!window.confirm(`Remove admin from "${username}"?`)) return;
    try { await demoteAdmin(room.id, userId); await loadData(); } catch (err) { setError(err.message); }
  }

  async function handleInvite(e) {
    e.preventDefault();
    setError('');
    try {
      await inviteUser(room.id, inviteUsername);
      setInviteUsername('');
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleSaveSettings(e) {
    e.preventDefault();
    setError('');
    try {
      await updateRoom(room.id, settingsName, settingsDesc, settingsPublic);
      onClose();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleDelete() {
    if (!window.confirm(`Delete room "${room.name}"?`)) return;
    await deleteRoom(room.id);
    onDeleted();
  }

  const admins = members.filter(m => m.role === 'ADMIN' || m.role === 'OWNER');
  const regularMembers = members.filter(m => m.role === 'MEMBER');

  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <div className={styles.titleBar}>
          <h2>Manage: {room.name}</h2>
          <button onClick={onClose}>✕</button>
        </div>

        <div className={styles.tabs}>
          {TABS.map(t => (
            <button
              key={t}
              className={`${styles.tab} ${tab === t ? styles.active : ''}`}
              onClick={() => setTab(t)}
            >
              {t}
            </button>
          ))}
        </div>

        <div className={styles.content}>
          {tab === 'Members' && (
            <ul className={styles.list}>
              {regularMembers.map(m => (
                <li key={m.userId} className={styles.row}>
                  <span>{m.username}</span>
                  <div className={styles.actions}>
                    {isOwner && (
                      <button onClick={() => handlePromote(m.userId, m.username)}>Make Admin</button>
                    )}
                    <button onClick={() => handleBan(m.userId, m.username)}>Ban</button>
                  </div>
                </li>
              ))}
              {regularMembers.length === 0 && <li className={styles.empty}>No regular members</li>}
            </ul>
          )}

          {tab === 'Admins' && (
            <ul className={styles.list}>
              {admins.map(m => (
                <li key={m.userId} className={styles.row}>
                  <span>{m.username} <em>({m.role.toLowerCase()})</em></span>
                  {isOwner && m.role === 'ADMIN' && (
                    <button onClick={() => handleDemote(m.userId, m.username)}>Demote</button>
                  )}
                </li>
              ))}
            </ul>
          )}

          {tab === 'Banned' && (
            <ul className={styles.list}>
              {bans.map(b => (
                <li key={b.userId} className={styles.row}>
                  <div className={styles.banInfo}>
                    <span className={styles.banUsername}>{b.username}</span>
                    <span className={styles.banMeta}>
                      banned by {b.bannedByUsername} · {new Date(b.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <button onClick={() => handleUnban(b.userId, b.username)}>Unban</button>
                </li>
              ))}
              {bans.length === 0 && <li className={styles.empty}>No bans</li>}
            </ul>
          )}

          {tab === 'Invitations' && (
            <form onSubmit={handleInvite} className={styles.inviteForm}>
              <input
                value={inviteUsername}
                onChange={e => setInviteUsername(e.target.value)}
                placeholder="Username to invite"
                required
              />
              <button type="submit">Invite</button>
            </form>
          )}

          {tab === 'Settings' && isOwner && (
            <form onSubmit={handleSaveSettings} className={styles.settingsForm}>
              <label>
                Name
                <input value={settingsName} onChange={e => setSettingsName(e.target.value)} required />
              </label>
              <label>
                Description
                <input value={settingsDesc} onChange={e => setSettingsDesc(e.target.value)} />
              </label>
              <label className={styles.checkboxRow}>
                <input
                  type="checkbox"
                  checked={settingsPublic}
                  onChange={e => setSettingsPublic(e.target.checked)}
                />
                Public
              </label>
              <div className={styles.settingsActions}>
                <button type="submit">Save</button>
                <button type="button" className={styles.danger} onClick={handleDelete}>
                  Delete Room
                </button>
              </div>
            </form>
          )}

          {error && <p className={styles.error}>{error}</p>}
        </div>
      </div>
    </div>
  );
}
