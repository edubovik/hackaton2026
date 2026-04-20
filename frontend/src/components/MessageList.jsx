import { useEffect, useRef } from 'react';
import { MessageItem } from './MessageItem';
import styles from './MessageList.module.css';

export function MessageList({ messages, hasMore, loading, onLoadMore, currentUserId, isRoomAdmin, onReply, onMessageUpdated }) {
  const topSentinelRef = useRef(null);
  const bottomRef = useRef(null);
  const isFirstLoad = useRef(true);

  // Auto-scroll to bottom on first load and new messages from self
  useEffect(() => {
    if (isFirstLoad.current && messages.length > 0) {
      bottomRef.current?.scrollIntoView();
      isFirstLoad.current = false;
    }
  }, [messages]);

  // Infinite scroll: load older messages when sentinel enters viewport
  useEffect(() => {
    if (!topSentinelRef.current || !hasMore) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) onLoadMore(); },
      { threshold: 0.1 }
    );
    observer.observe(topSentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, onLoadMore]);

  return (
    <div className={styles.list}>
      <div ref={topSentinelRef} className={styles.sentinel}>
        {loading && <span className={styles.loadingText}>Loading…</span>}
      </div>
      {messages.map((msg) => (
        <MessageItem
          key={msg.id}
          message={msg}
          currentUserId={currentUserId}
          isRoomAdmin={isRoomAdmin}
          onReply={onReply}
          onUpdated={onMessageUpdated}
        />
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
