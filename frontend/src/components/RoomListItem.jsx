import styles from './RoomListItem.module.css';

export function RoomListItem({ room, onJoin, onSelect }) {
  return (
    <li className={styles.item}>
      <button className={styles.name} onClick={() => onSelect(room)}>
        {room.name}
      </button>
      {room.description && <span className={styles.desc}>{room.description}</span>}
      <div className={styles.meta}>
        {room.memberCount > 0 && (
          <span className={styles.count}>{room.memberCount} member{room.memberCount !== 1 ? 's' : ''}</span>
        )}
        <button className={styles.join} onClick={() => onJoin(room.id)}>Join</button>
      </div>
    </li>
  );
}
