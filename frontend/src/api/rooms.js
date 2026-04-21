import { apiFetch } from './http';

export const getMyRooms = () => apiFetch('/rooms/my');

export const createRoom = (name, description, isPublic) =>
  apiFetch('/rooms', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, description, isPublic }) });

export function listRooms(search = '', page = 0, size = 20) {
  const params = new URLSearchParams({ page, size });
  if (search) params.set('search', search);
  return apiFetch(`/rooms?${params}`);
}

export const getRoom = (id) => apiFetch(`/rooms/${id}`);

export const updateRoom = (id, name, description, isPublic) =>
  apiFetch(`/rooms/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, description, isPublic }) });

export const deleteRoom = (id) => apiFetch(`/rooms/${id}`, { method: 'DELETE' });

export const joinRoom = (id) => apiFetch(`/rooms/${id}/join`, { method: 'POST' });

export const leaveRoom = (id) => apiFetch(`/rooms/${id}/leave`, { method: 'DELETE' });

export const listMembers = (id) => apiFetch(`/rooms/${id}/members`);

export const banMember = (roomId, userId) => apiFetch(`/rooms/${roomId}/members/${userId}/ban`, { method: 'POST' });

export const unbanMember = (roomId, userId) => apiFetch(`/rooms/${roomId}/members/${userId}/ban`, { method: 'DELETE' });

export const listBans = (roomId) => apiFetch(`/rooms/${roomId}/bans`);

export const promoteAdmin = (roomId, userId) => apiFetch(`/rooms/${roomId}/admins/${userId}`, { method: 'POST' });

export const demoteAdmin = (roomId, userId) => apiFetch(`/rooms/${roomId}/admins/${userId}`, { method: 'DELETE' });

export const inviteUser = (roomId, username) =>
  apiFetch(`/rooms/${roomId}/invitations`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username }) });

export const acceptInvitation = (roomId) => apiFetch(`/rooms/${roomId}/invitations/accept`, { method: 'POST' });
