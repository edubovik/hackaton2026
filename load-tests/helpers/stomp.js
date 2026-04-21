/**
 * Minimal STOMP 1.2 frame builder / parser for k6 WebSocket tests.
 * All frames are plain-text strings terminated with a null byte (\0).
 */

const NULL = '\x00';

/** STOMP CONNECT — no auth needed; token was passed as WS query param */
export function connectFrame() {
  return `CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n${NULL}`;
}

/** STOMP SUBSCRIBE */
export function subscribeFrame(subId, destination) {
  return `SUBSCRIBE\nid:${subId}\ndestination:${destination}\n\n${NULL}`;
}

/**
 * STOMP SEND with a JSON body.
 * @param {string} destination  e.g. /app/chat.room.42
 * @param {object|string} body  JS object (will be JSON.stringify'd) or raw string
 */
export function sendFrame(destination, body) {
  const encoded = typeof body === 'string' ? body : JSON.stringify(body);
  return `SEND\ndestination:${destination}\ncontent-type:application/json\ncontent-length:${encoded.length}\n\n${encoded}${NULL}`;
}

/** STOMP SEND to presence heartbeat endpoint (no body) */
export function heartbeatFrame() {
  return `SEND\ndestination:/app/presence/heartbeat\ncontent-length:0\n\n${NULL}`;
}

/** STOMP DISCONNECT */
export function disconnectFrame() {
  return `DISCONNECT\n\n${NULL}`;
}

/**
 * Parse a raw STOMP frame string into { command, headers, body }.
 * Returns { command: '' } for empty / heartbeat frames.
 */
export function parseFrame(raw) {
  if (!raw || raw.trim() === '') return { command: '', headers: {}, body: '' };

  const nullIdx = raw.indexOf(NULL);
  const frameStr = nullIdx >= 0 ? raw.substring(0, nullIdx) : raw;

  const sepIdx = frameStr.indexOf('\n\n');
  const headerSection = sepIdx >= 0 ? frameStr.substring(0, sepIdx) : frameStr;
  const body = sepIdx >= 0 ? frameStr.substring(sepIdx + 2) : '';

  const lines = headerSection.split('\n');
  const command = lines[0].trim();
  const headers = {};

  for (let i = 1; i < lines.length; i++) {
    const colonIdx = lines[i].indexOf(':');
    if (colonIdx > 0) {
      headers[lines[i].substring(0, colonIdx).trim()] = lines[i].substring(colonIdx + 1).trim();
    }
  }

  return { command, headers, body };
}
