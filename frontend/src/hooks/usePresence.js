import { useEffect, useRef, useState } from 'react';
import { connect, disconnect, subscribe, publish } from '../api/socket';

const HEARTBEAT_INTERVAL_MS = 30_000;
const AFK_IDLE_MS = 55_000;

export function usePresence() {
  const [presenceMap, setPresenceMap] = useState({});
  const heartbeatRef = useRef(null);
  const lastActivityRef = useRef(Date.now());
  const isActiveRef = useRef(true);
  const subscriptionRef = useRef(null);

  function recordActivity() {
    lastActivityRef.current = Date.now();
    isActiveRef.current = true;
  }

  useEffect(() => {
    const activityEvents = ['mousemove', 'keydown', 'click', 'touchstart'];
    activityEvents.forEach((e) => window.addEventListener(e, recordActivity));

    connect(
      () => {
        subscriptionRef.current = subscribe('/topic/presence', (update) => {
          setPresenceMap((prev) => ({ ...prev, [update.userId]: update.state }));
        });

        heartbeatRef.current = setInterval(() => {
          const idle = Date.now() - lastActivityRef.current;
          if (idle < AFK_IDLE_MS) {
            publish('/app/presence/heartbeat', {});
          }
        }, HEARTBEAT_INTERVAL_MS);

        publish('/app/presence/heartbeat', {});
      },
      () => {
        clearInterval(heartbeatRef.current);
      }
    );

    return () => {
      activityEvents.forEach((e) => window.removeEventListener(e, recordActivity));
      clearInterval(heartbeatRef.current);
      if (subscriptionRef.current) subscriptionRef.current.unsubscribe();
      disconnect();
    };
  }, []);

  return presenceMap;
}
