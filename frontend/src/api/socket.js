import { Client } from '@stomp/stompjs';

let client = null;
let connected = false;
const connectionListeners = new Set();

function setConnected(value) {
  connected = value;
  connectionListeners.forEach((cb) => cb(value));
}

// Derive WebSocket URL from the current page origin so the connection goes
// through the Nginx proxy (/ws → backend:8080) and avoids cross-origin rejection.
const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
const WS_URL = import.meta.env.VITE_WS_URL || `${proto}//${window.location.host}/ws`;

export function connect() {
  if (client) return;
  client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 5000,
    // Refresh the access-token cookie before every connect/reconnect so the
    // WebSocket handshake always has a valid token (access tokens expire in 15 min).
    beforeConnect: async () => {
      await fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'include' }).catch(() => {});
    },
    onConnect: () => setConnected(true),
    onDisconnect: () => setConnected(false),
    onStompError: () => setConnected(false),
  });
  client.activate();
}

export function disconnect() {
  if (client) {
    client.deactivate();
    client = null;
  }
  setConnected(false);
}

export function onConnectionChange(cb) {
  connectionListeners.add(cb);
  return () => connectionListeners.delete(cb);
}

export function isConnected() {
  return connected;
}

export function subscribe(destination, callback) {
  if (!client || !client.connected) return null;
  return client.subscribe(destination, (message) => {
    callback(JSON.parse(message.body));
  });
}

export function publish(destination, body) {
  if (!client || !client.connected) return;
  client.publish({ destination, body: JSON.stringify(body) });
}

export function subscribeRoom(roomId, callback) {
  return subscribe(`/topic/room.${roomId}`, callback);
}

export function subscribeDm(userId, callback) {
  return subscribe(`/queue/user.${userId}`, callback);
}

export function sendRoomMessage(roomId, content, replyToId = null) {
  publish(`/app/chat.room.${roomId}`, { content, replyToId });
}

export function sendDmMessage(partnerId, content, replyToId = null) {
  publish(`/app/chat.dm.${partnerId}`, { content, replyToId });
}

export function getClient() {
  return client;
}
