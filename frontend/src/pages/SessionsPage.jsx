import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getSessions, deleteSession } from '../api/auth';
import styles from './SessionsPage.module.css';

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSessions()
      .then(setSessions)
      .catch(() => setError('Failed to load sessions'))
      .finally(() => setLoading(false));
  }, []);

  async function handleRevoke(id) {
    try {
      await deleteSession(id);
      setSessions((prev) => prev.filter((s) => s.id !== id));
    } catch {
      setError('Failed to revoke session');
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1>Active Sessions</h1>
        {error && <div className={styles.error}>{error}</div>}
        {loading ? (
          <p>Loading…</p>
        ) : sessions.length === 0 ? (
          <p className={styles.empty}>No active sessions.</p>
        ) : (
          <ul className={styles.list}>
            {sessions.map((session) => (
              <li key={session.id} className={styles.item}>
                <div className={styles.info}>
                  <span className={styles.agent}>{session.userAgent || 'Unknown device'}</span>
                  <span className={styles.meta}>
                    {session.ipAddress || 'Unknown IP'} &middot;{' '}
                    {new Date(session.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <button
                  className={styles.revoke}
                  onClick={() => handleRevoke(session.id)}
                  aria-label="Revoke session"
                >
                  Revoke
                </button>
              </li>
            ))}
          </ul>
        )}
        <Link className={styles.link} to="/">Back to Chat</Link>
      </div>
    </div>
  );
}
