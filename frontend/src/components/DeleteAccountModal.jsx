import { useState } from 'react';
import { deleteAccount } from '../api/auth';
import styles from './DeleteAccountModal.module.css';

export default function DeleteAccountModal({ onClose, onDeleted }) {
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!password) {
      setError('Password is required');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await deleteAccount(password);
      onDeleted();
    } catch (err) {
      setError(err.message || 'Failed to delete account');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-label="Delete account">
      <div className={styles.modal}>
        <h2>Delete Account</h2>
        <p className={styles.warning}>
          This action is permanent and cannot be undone. All your rooms and data will be deleted.
        </p>
        {error && <div className={styles.error}>{error}</div>}
        <form onSubmit={handleSubmit} noValidate>
          <div className={styles.field}>
            <label htmlFor="password">Confirm your password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>
          <div className={styles.actions}>
            <button type="button" className={styles.cancel} onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className={styles.danger} disabled={loading}>
              {loading ? 'Deleting…' : 'Delete My Account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
