import { useState, useEffect, useCallback } from 'react';
import { fetchUnreadCounts, markRoomRead, markDmRead } from '../api/messages';

export function useUnread() {
  const [counts, setCounts] = useState([]);

  const refresh = useCallback(async () => {
    try {
      const data = await fetchUnreadCounts();
      setCounts(data);
    } catch {
      // silently ignore — not critical
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const clearRoom = useCallback(async (roomId) => {
    await markRoomRead(roomId);
    setCounts((prev) => prev.filter((c) => c.roomId !== roomId));
  }, []);

  const clearDm = useCallback(async (partnerId) => {
    await markDmRead(partnerId);
    setCounts((prev) => prev.filter((c) => c.partnerId !== partnerId));
  }, []);

  const getCountForRoom = useCallback(
    (roomId) => counts.find((c) => c.roomId === roomId)?.count ?? 0,
    [counts]
  );

  const getCountForDm = useCallback(
    (partnerId) => counts.find((c) => c.partnerId === partnerId)?.count ?? 0,
    [counts]
  );

  return { counts, refresh, clearRoom, clearDm, getCountForRoom, getCountForDm };
}
