import { useEffect, useRef, useState } from 'react';
import { onConnectionChange, isConnected, subscribe, publish } from '../api/socket';

const HEARTBEAT_INTERVAL_MS = 30_000;
const AFK_IDLE_MS = 55_000;

export function usePresence() {
  const [presenceMap, setPresenceMap] = useState({});
  const heartbeatRef = useRef(null);
  const lastActivityRef = useRef(Date.now());
  const subscriptionRef = useRef(null);

  function recordActivity() {
    lastActivityRef.current = Date.now();
  }

  useEffect(() => {
    const activityEvents = ['mousemove', 'keydown', 'click', 'touchstart'];
    activityEvents.forEach((e) => window.addEventListener(e, recordActivity));

    function setupPresence(conn) {
      if (!conn) {
        clearInterval(heartbeatRef.current);
        if (subscriptionRef.current) {
          subscriptionRef.current.unsubscribe();
          subscriptionRef.current = null;
        }
        return;
      }
      subscriptionRef.current = subscribe('/topic/presence', (update) => {
        setPresenceMap((prev) => ({ ...prev, [update.userId]: update.state }));
      });
      heartbeatRef.current = setInterval(() => {
        const idle = Date.now() - lastActivityRef.current;
        if (idle < AFK_IDLE_MS) publish('/app/presence/heartbeat', {});
      }, HEARTBEAT_INTERVAL_MS);
      publish('/app/presence/heartbeat', {});
    }

    if (isConnected()) setupPresence(true);
    const unsub = onConnectionChange(setupPresence);

    return () => {
      activityEvents.forEach((e) => window.removeEventListener(e, recordActivity));
      clearInterval(heartbeatRef.current);
      if (subscriptionRef.current) subscriptionRef.current.unsubscribe();
      unsub();
    };
  }, []);

  return presenceMap;
}
