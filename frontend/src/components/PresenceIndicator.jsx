import styles from './PresenceIndicator.module.css';

const ICONS = {
  ONLINE: { symbol: '●', label: 'Online' },
  AFK: { symbol: '◐', label: 'Away' },
  OFFLINE: { symbol: '○', label: 'Offline' },
};

export function PresenceIndicator({ state = 'OFFLINE' }) {
  const { symbol, label } = ICONS[state] ?? ICONS.OFFLINE;
  return (
    <span
      className={`${styles.indicator} ${styles[state.toLowerCase()]}`}
      title={label}
      aria-label={label}
    >
      {symbol}
    </span>
  );
}
