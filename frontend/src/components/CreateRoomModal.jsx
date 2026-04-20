import { useState } from 'react';
import { createRoom } from '../api/rooms';
import styles from './CreateRoomModal.module.css';

export function CreateRoomModal({ onCreated, onClose }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isPublic, setIsPublic] = useState(true);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const room = await createRoom(name, description, isPublic);
      onCreated(room);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <h2>Create Room</h2>
        <form onSubmit={handleSubmit}>
          <label>
            Name
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              required
              maxLength={100}
            />
          </label>
          <label>
            Description
            <input
              value={description}
              onChange={e => setDescription(e.target.value)}
            />
          </label>
          <label className={styles.checkbox}>
            <input
              type="checkbox"
              checked={isPublic}
              onChange={e => setIsPublic(e.target.checked)}
            />
            Public room
          </label>
          {error && <p className={styles.error}>{error}</p>}
          <div className={styles.actions}>
            <button type="button" onClick={onClose}>Cancel</button>
            <button type="submit">Create</button>
          </div>
        </form>
      </div>
    </div>
  );
}
