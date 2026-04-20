import styles from './ReplyBar.module.css';

export function ReplyBar({ replyTo, onCancel }) {
  if (!replyTo) return null;
  return (
    <div className={styles.bar}>
      <span className={styles.label}>
        Replying to <strong>{replyTo.senderUsername}</strong>:{' '}
        <em>{replyTo.content.slice(0, 60)}{replyTo.content.length > 60 ? '…' : ''}</em>
      </span>
      <button className={styles.cancel} onClick={onCancel} aria-label="Cancel reply">×</button>
    </div>
  );
}
