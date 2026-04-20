import styles from './RoomListItem.module.css';

export function RoomListItem({ room, onJoin, onSelect }) {
  return (
    <li className={styles.item}>
      <button className={styles.name} onClick={() => onSelect(room)}>
        {room.name}
      </button>
      {room.description && <span className={styles.desc}>{room.description}</span>}
      <button className={styles.join} onClick={() => onJoin(room.id)}>Join</button>
    </li>
  );
}
