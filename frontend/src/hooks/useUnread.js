import { useState, useEffect, useCallback, useRef } from 'react';
import { fetchUnreadCounts, markRoomRead, markDmRead } from '../api/messages';
import { onConnectionChange, isConnected, subscribe } from '../api/socket';

export function useUnread({ rooms = [], userId, activeRoomId, activeDmPartnerId } = {}) {
  const [counts, setCounts] = useState([]);
  const activeRoomIdRef = useRef(activeRoomId);
  const activeDmPartnerIdRef = useRef(activeDmPartnerId);

  useEffect(() => { activeRoomIdRef.current = activeRoomId; }, [activeRoomId]);
  useEffect(() => { activeDmPartnerIdRef.current = activeDmPartnerId; }, [activeDmPartnerId]);

  const refresh = useCallback(async () => {
    try {
      const data = await fetchUnreadCounts();
      setCounts(data);
    } catch {
      // not critical
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // Subscribe to all joined room topics and the personal DM queue for live bump
  useEffect(() => {
    if (!userId) return;

    let subs = [];

    function setup(conn) {
      subs.forEach((s) => s?.unsubscribe());
      subs = [];
      if (!conn) return;

      rooms.forEach((room) => {
        const sub = subscribe(`/topic/room.${room.id}`, () => {
          if (activeRoomIdRef.current !== room.id) {
            setCounts((prev) => {
              const existing = prev.find((c) => c.roomId === room.id);
              if (existing) {
                return prev.map((c) => c.roomId === room.id ? { ...c, count: c.count + 1 } : c);
              }
              return [...prev, { roomId: room.id, count: 1 }];
            });
          }
        });
        if (sub) subs.push(sub);
      });

      const dmSub = subscribe(`/topic/user.${userId}`, (msg) => {
        if (msg.senderId !== undefined && msg.senderId !== userId) {
          const partnerId = msg.senderId;
          if (activeDmPartnerIdRef.current !== partnerId) {
            setCounts((prev) => {
              const existing = prev.find((c) => c.partnerId === partnerId);
              if (existing) {
                return prev.map((c) => c.partnerId === partnerId ? { ...c, count: c.count + 1 } : c);
              }
              return [...prev, { partnerId, count: 1 }];
            });
          }
        }
      });
      if (dmSub) subs.push(dmSub);
    }

    if (isConnected()) setup(true);
    const unsub = onConnectionChange(setup);

    return () => {
      subs.forEach((s) => s?.unsubscribe());
      unsub();
    };
  }, [rooms, userId]);

  const clearRoom = useCallback(async (roomId) => {
    try { await markRoomRead(roomId); } catch { /* ignore */ }
    setCounts((prev) => prev.filter((c) => c.roomId !== roomId));
  }, []);

  const clearDm = useCallback(async (partnerId) => {
    try { await markDmRead(partnerId); } catch { /* ignore */ }
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
