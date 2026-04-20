import styles from './RoomPanel.module.css';

export function RoomPanel({ rooms, selectedRoomId, onSelect, onOpenCatalog }) {
  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <h3>Rooms</h3>
        <button onClick={onOpenCatalog}>Browse</button>
      </div>
      <ul className={styles.list}>
        {rooms.map(r => (
          <li
            key={r.id}
            className={`${styles.item} ${r.id === selectedRoomId ? styles.active : ''}`}
            onClick={() => onSelect(r)}
          >
            {r.name}
          </li>
        ))}
        {rooms.length === 0 && <li className={styles.empty}>No rooms joined yet</li>}
      </ul>
    </div>
  );
}
