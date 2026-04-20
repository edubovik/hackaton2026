import { PresenceIndicator } from './PresenceIndicator';
import styles from './FriendListItem.module.css';

export function FriendListItem({ friend, onRemove, onBan }) {
  return (
    <li className={styles.item}>
      <PresenceIndicator state={friend.presence} />
      <span className={styles.username}>{friend.username}</span>
      <div className={styles.actions}>
        <button onClick={() => onRemove(friend.userId)}>Remove</button>
        <button onClick={() => onBan(friend.userId)}>Ban</button>
      </div>
    </li>
  );
}
