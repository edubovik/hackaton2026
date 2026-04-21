/**
 * k6 load test — full user simulation for the chat application.
 *
 * Each VU: login → join room → STOMP connect → subscribe → send messages
 *          every 10-15s → heartbeat every 30s → stay connected.
 *
 * Run with:
 *   docker run --rm --network host \
 *     -v "$(pwd)/load-tests:/scripts" \
 *     [-e SMOKE=true] \
 *     grafana/k6 run /scripts/scenario.js \
 *     --out json=/scripts/reports/report-$(date +%s).json
 *
 * Prerequisites: run setup.sh first (creates users.json + room_id.txt).
 */

import ws from 'k6/ws';
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

import {
  connectFrame,
  subscribeFrame,
  sendFrame,
  heartbeatFrame,
  disconnectFrame,
  parseFrame,
} from './helpers/stomp.js';

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const wsConnectTime         = new Trend('ws_connect_time', true);         // ms WS open → STOMP CONNECTED
const messageDeliveryLatency = new Trend('message_delivery_latency', true); // ms SEND → own msg received back
const messagesSent          = new Counter('messages_sent');
const messagesReceived      = new Counter('messages_received');
const loginSuccessRate      = new Rate('login_success');

// ---------------------------------------------------------------------------
// Init context — runs once per VU before default()
// ---------------------------------------------------------------------------
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS_URL   = __ENV.WS_URL   || 'ws://localhost:8080/ws';
const IS_SMOKE = __ENV.SMOKE === 'true';

// Socket stays open for slightly longer than the longest possible test stage.
// Full: 2m ramp + 5m sustain + 1m down = 8m.  Smoke: 30s+1m+30s = 2m.
const SOCKET_TIMEOUT_MS = IS_SMOKE ? 140_000 : 500_000;

const roomId = Number(open('./room_id.txt').trim());

const users = new SharedArray('users', function () {
  return JSON.parse(open('./users.json'));
});

// ---------------------------------------------------------------------------
// Test options
// ---------------------------------------------------------------------------
export const options = {
  stages: IS_SMOKE
    ? [
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 10 },
        { duration: '30s', target: 0  },
      ]
    : [
        { duration: '2m', target: 300 },
        { duration: '5m', target: 300 },
        { duration: '1m', target: 0   },
      ],

  thresholds: {
    // REST — scope login separately; join may 400 for the room owner (expected)
    'http_req_failed{name:login}':        ['rate<0.01'],
    'http_req_duration{name:login}':      ['p(95)<2000'],
    'http_req_duration{name:join_room}':  ['p(95)<1000'],
    // WebSocket
    'ws_connect_time':                    ['p(95)<5000'],
    // Message delivery — §3.2 requires <3 s
    'message_delivery_latency':           ['p(95)<3000'],
    // Login must succeed for virtually all VUs
    'login_success':                      ['rate>0.99'],
  },
};

// ---------------------------------------------------------------------------
// VU entry point
// ---------------------------------------------------------------------------
export default function () {
  const vuIdx  = (__VU - 1) % users.length;
  const user   = users[vuIdx];
  const vuId   = __VU; // used as the marker prefix for latency measurement

  // ── 1. Login ──────────────────────────────────────────────────────────────
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );

  const loginOk = check(loginRes, {
    'login: status 200':    (r) => r.status === 200,
    'login: has accessToken': (r) => Boolean(r.json() && r.json().accessToken),
  });
  loginSuccessRate.add(loginOk ? 1 : 0);

  if (!loginOk) {
    console.error(`VU ${vuId}: login failed (${loginRes.status}): ${loginRes.body}`);
    return;
  }

  const accessToken = loginRes.json().accessToken;
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${accessToken}`,
  };

  // ── 2. Join room ──────────────────────────────────────────────────────────
  const joinRes = http.post(
    `${BASE_URL}/api/v1/rooms/${roomId}/join`,
    null,
    { headers: authHeaders, tags: { name: 'join_room' } }
  );
  // 200 = joined now, 409 = conflict, 400 = "Already a member" (owner of the room) — all acceptable
  check(joinRes, { 'room join: ok': (r) => r.status === 200 || r.status === 409 || r.status === 400 });

  // ── 3. WebSocket + STOMP ──────────────────────────────────────────────────
  const wsUrl = `${WS_URL}?token=${encodeURIComponent(accessToken)}`;
  const wsConnectStart = Date.now();

  // pendingMessages maps marker string → send timestamp (ms)
  const pendingMessages = {};

  const socketRes = ws.connect(wsUrl, {}, function (socket) {

    socket.on('open', function () {
      socket.send(connectFrame());
    });

    socket.on('message', function (data) {
      const frame = parseFrame(data);

      // ── STOMP handshake complete ──
      if (frame.command === 'CONNECTED') {
        wsConnectTime.add(Date.now() - wsConnectStart);

        // Subscribe to the shared load-test room
        socket.send(subscribeFrame('sub-room', `/topic/room.${roomId}`));

        // Start message send loop (10-15 s random interval)
        scheduleNextMessage(socket, roomId, vuId, pendingMessages);

        // Presence heartbeat every 30 s
        socket.setInterval(function () {
          socket.send(heartbeatFrame());
        }, 30_000);
      }

      // ── Application message received ──
      if (frame.command === 'MESSAGE') {
        messagesReceived.add(1);

        // Measure delivery latency for messages sent by this VU
        if (frame.body) {
          try {
            const dto = JSON.parse(frame.body);
            if (dto && dto.content) {
              const match = dto.content.match(/^lt_(\d+)_(\d+)$/);
              if (match && Number(match[1]) === vuId) {
                messageDeliveryLatency.add(Date.now() - Number(match[2]));
              }
            }
          } catch (_) { /* non-JSON frame body — ignore */ }
        }
      }

      if (frame.command === 'ERROR') {
        console.error(`VU ${vuId}: STOMP ERROR — ${frame.body}`);
      }
    });

    socket.on('error', function (e) {
      console.error(`VU ${vuId}: WS error — ${e.error()}`);
    });

    // Graceful close once the test stage window has passed
    socket.setTimeout(function () {
      socket.send(disconnectFrame());
      socket.close();
    }, SOCKET_TIMEOUT_MS);
  });

  check(socketRes, { 'ws: upgrade 101': (r) => r && r.status === 101 });
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Recursively schedule the next room message after a random 10-15 s delay.
 * Embeds a timestamp marker so the receiving handler can measure round-trip latency.
 */
function scheduleNextMessage(socket, roomId, vuId, pendingMessages) {
  const delay = 10_000 + Math.floor(Math.random() * 5_000); // 10 000 – 15 000 ms

  socket.setTimeout(function () {
    const sendTs = Date.now();
    const marker = `lt_${vuId}_${sendTs}`;
    pendingMessages[marker] = sendTs;

    socket.send(
      sendFrame(`/app/chat.room.${roomId}`, { content: marker, replyToId: null })
    );
    messagesSent.add(1);

    // Schedule the next one
    scheduleNextMessage(socket, roomId, vuId, pendingMessages);
  }, delay);
}
