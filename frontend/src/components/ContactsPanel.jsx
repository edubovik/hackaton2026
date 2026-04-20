import { useEffect, useState } from 'react';
import {
  getFriends,
  getIncomingRequests,
  acceptRequest,
  rejectRequest,
  removeFriend,
  banUser,
  sendFriendRequest,
} from '../api/contacts';
import { FriendListItem } from './FriendListItem';
import { FriendRequestItem } from './FriendRequestItem';
import styles from './ContactsPanel.module.css';

export function ContactsPanel() {
  const [friends, setFriends] = useState([]);
  const [requests, setRequests] = useState([]);
  const [addUsername, setAddUsername] = useState('');
  const [addMessage, setAddMessage] = useState('');
  const [error, setError] = useState('');

  async function load() {
    const [f, r] = await Promise.all([getFriends(), getIncomingRequests()]);
    setFriends(f);
    setRequests(r);
  }

  useEffect(() => { load(); }, []);

  async function handleAccept(id) {
    await acceptRequest(id);
    await load();
  }

  async function handleReject(id) {
    await rejectRequest(id);
    await load();
  }

  async function handleRemove(userId) {
    await removeFriend(userId);
    await load();
  }

  async function handleBan(userId) {
    await banUser(userId);
    await load();
  }

  async function handleSendRequest(e) {
    e.preventDefault();
    setError('');
    try {
      await sendFriendRequest(addUsername, addMessage);
      setAddUsername('');
      setAddMessage('');
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className={styles.panel}>
      <section>
        <h3>Add Friend</h3>
        <form onSubmit={handleSendRequest} className={styles.addForm}>
          <input
            value={addUsername}
            onChange={e => setAddUsername(e.target.value)}
            placeholder="Username"
            required
          />
          <input
            value={addMessage}
            onChange={e => setAddMessage(e.target.value)}
            placeholder="Message (optional)"
          />
          <button type="submit">Send Request</button>
        </form>
        {error && <p className={styles.error}>{error}</p>}
      </section>

      {requests.length > 0 && (
        <section>
          <h3>Pending Requests</h3>
          <ul className={styles.list}>
            {requests.map(r => (
              <FriendRequestItem
                key={r.id}
                request={r}
                onAccept={handleAccept}
                onReject={handleReject}
              />
            ))}
          </ul>
        </section>
      )}

      <section>
        <h3>Friends ({friends.length})</h3>
        <ul className={styles.list}>
          {friends.map(f => (
            <FriendListItem
              key={f.userId}
              friend={f}
              onRemove={handleRemove}
              onBan={handleBan}
            />
          ))}
          {friends.length === 0 && <li className={styles.empty}>No friends yet</li>}
        </ul>
      </section>
    </div>
  );
}
