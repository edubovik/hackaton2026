import { useState, useEffect, useCallback, useRef } from 'react';
import { fetchRoomHistory, fetchDmHistory } from '../api/messages';
import { subscribeRoom, subscribeDm, onConnectionChange, isConnected } from '../api/socket';

export function useMessages({ roomId, partnerId, currentUserId }) {
  const [messages, setMessages] = useState([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const oldestIdRef = useRef(null);
  const [socketConnected, setSocketConnected] = useState(isConnected);

  useEffect(() => {
    return onConnectionChange(setSocketConnected);
  }, []);

  const upsertMessage = useCallback((msg) => {
    setMessages((prev) => {
      const idx = prev.findIndex((m) => m.id === msg.id);
      if (idx >= 0) {
        const updated = [...prev];
        updated[idx] = msg;
        return updated;
      }
      return [...prev, msg];
    });
  }, []);

  const loadHistory = useCallback(async (cursor) => {
    setLoading(true);
    try {
      const page = roomId
        ? await fetchRoomHistory(roomId, cursor)
        : await fetchDmHistory(partnerId, cursor);
      const older = [...page.messages].reverse();
      setMessages((prev) => [...older, ...prev]);
      setHasMore(page.hasMore);
      if (older.length > 0) oldestIdRef.current = older[0].id;
    } finally {
      setLoading(false);
    }
  }, [roomId, partnerId]);

  // Load history when conversation changes
  useEffect(() => {
    if (!roomId && !partnerId) return;
    setMessages([]);
    setHasMore(false);
    oldestIdRef.current = null;
    loadHistory(null);
  }, [roomId, partnerId, loadHistory]);

  // Subscribe only when socket is connected; re-subscribes automatically on reconnect
  useEffect(() => {
    if ((!roomId && !partnerId) || !socketConnected) return;
    const sub = roomId
      ? subscribeRoom(roomId, upsertMessage)
      : subscribeDm(currentUserId, upsertMessage);
    return () => sub && sub.unsubscribe();
  }, [roomId, partnerId, currentUserId, socketConnected, upsertMessage]);

  const loadMore = useCallback(() => {
    if (!loading && hasMore) loadHistory(oldestIdRef.current);
  }, [loading, hasMore, loadHistory]);

  return { messages, hasMore, loading, loadMore };
}
