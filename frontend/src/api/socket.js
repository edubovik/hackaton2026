import { Client } from '@stomp/stompjs';
import { getAccessToken, getRefreshToken, storeTokens } from './tokenStorage';

let client = null;
let connected = false;
const connectionListeners = new Set();

function setConnected(value) {
  connected = value;
  connectionListeners.forEach((cb) => cb(value));
}

const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
const WS_BASE = import.meta.env.VITE_WS_URL || `${proto}//${window.location.host}/ws`;

function getWsUrl() {
  const token = getAccessToken();
  return token ? `${WS_BASE}?token=${encodeURIComponent(token)}` : WS_BASE;
}

async function refreshBeforeConnect() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return;
  try {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (res.ok) {
      const data = await res.json();
      storeTokens(data.accessToken, null, false);
      if (client) client.brokerURL = getWsUrl();
    }
  } catch (_) {}
}

export function connect() {
  if (client) return;
  client = new Client({
    brokerURL: getWsUrl(),
    reconnectDelay: 5000,
    beforeConnect: refreshBeforeConnect,
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
