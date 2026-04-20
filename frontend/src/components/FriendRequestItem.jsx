import styles from './FriendRequestItem.module.css';

export function FriendRequestItem({ request, onAccept, onReject }) {
  return (
    <li className={styles.item}>
      <span className={styles.username}>{request.fromUsername}</span>
      {request.message && <span className={styles.message}>{request.message}</span>}
      <div className={styles.actions}>
        <button onClick={() => onAccept(request.id)}>Accept</button>
        <button onClick={() => onReject(request.id)}>Reject</button>
      </div>
    </li>
  );
}
