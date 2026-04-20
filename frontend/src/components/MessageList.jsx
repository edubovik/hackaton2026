import { useCallback, useEffect, useRef } from 'react';
import { MessageItem } from './MessageItem';
import styles from './MessageList.module.css';

export function MessageList({ messages, hasMore, loading, onLoadMore, currentUserId, isRoomAdmin, onReply, onMessageUpdated }) {
  const listRef = useRef(null);
  const topSentinelRef = useRef(null);
  const bottomRef = useRef(null);
  const isFirstLoad = useRef(true);
  const isAtBottomRef = useRef(true);

  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    function onScroll() {
      const { scrollTop, scrollHeight, clientHeight } = list;
      isAtBottomRef.current = scrollTop + clientHeight >= scrollHeight - 60;
    }
    list.addEventListener('scroll', onScroll, { passive: true });
    return () => list.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    if (messages.length === 0) return;
    if (isFirstLoad.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'instant' });
      isFirstLoad.current = false;
    } else if (isAtBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  useEffect(() => {
    if (!topSentinelRef.current || !hasMore) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) onLoadMore(); },
      { threshold: 0.1 }
    );
    observer.observe(topSentinelRef.current);
    return () => observer.disconnect();
  }, [hasMore, onLoadMore]);

  const findMessage = useCallback(
    (id) => messages.find((m) => m.id === id) ?? null,
    [messages]
  );

  return (
    <div className={styles.list} ref={listRef}>
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
          findMessage={findMessage}
        />
      ))}
      <div ref={bottomRef} />
    </div>
  );
}
