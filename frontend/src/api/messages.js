import { apiFetch } from './http';

export const fetchRoomHistory = (roomId, before) =>
  apiFetch(`/rooms/${roomId}/messages${before ? `?before=${before}` : ''}`);

export const fetchDmHistory = (partnerId, before) =>
  apiFetch(`/messages/dm/${partnerId}${before ? `?before=${before}` : ''}`);

export const editMessage = (id, content) =>
  apiFetch(`/messages/${id}`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content }) });

export const deleteMessage = (id) =>
  apiFetch(`/messages/${id}`, { method: 'DELETE' });

export const fetchUnreadCounts = () => apiFetch('/messages/unread');

export const markRoomRead = (roomId) => apiFetch(`/messages/unread/room/${roomId}`, { method: 'DELETE' });

export const markDmRead = (partnerId) => apiFetch(`/messages/unread/dm/${partnerId}`, { method: 'DELETE' });
