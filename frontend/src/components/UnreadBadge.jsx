import styles from './UnreadBadge.module.css';

export function UnreadBadge({ count }) {
  if (!count || count <= 0) return null;
  return <span className={styles.badge}>{count > 99 ? '99+' : count}</span>;
}
