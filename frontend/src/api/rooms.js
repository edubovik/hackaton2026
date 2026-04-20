const BASE = '/api/v1/rooms';

export async function createRoom(name, description, isPublic) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, isPublic }),
  });
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function listRooms(search = '', page = 0, size = 20) {
  const params = new URLSearchParams({ page, size });
  if (search) params.set('search', search);
  const res = await fetch(`${BASE}?${params}`);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function getRoom(id) {
  const res = await fetch(`${BASE}/${id}`);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function updateRoom(id, name, description, isPublic) {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description, isPublic }),
  });
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function deleteRoom(id) {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function joinRoom(id) {
  const res = await fetch(`${BASE}/${id}/join`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function leaveRoom(id) {
  const res = await fetch(`${BASE}/${id}/leave`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function listMembers(id) {
  const res = await fetch(`${BASE}/${id}/members`);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function banMember(roomId, userId) {
  const res = await fetch(`${BASE}/${roomId}/members/${userId}/ban`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function unbanMember(roomId, userId) {
  const res = await fetch(`${BASE}/${roomId}/members/${userId}/ban`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function listBans(roomId) {
  const res = await fetch(`${BASE}/${roomId}/bans`);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function promoteAdmin(roomId, userId) {
  const res = await fetch(`${BASE}/${roomId}/admins/${userId}`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function demoteAdmin(roomId, userId) {
  const res = await fetch(`${BASE}/${roomId}/admins/${userId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function inviteUser(roomId, username) {
  const res = await fetch(`${BASE}/${roomId}/invitations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username }),
  });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function acceptInvitation(roomId) {
  const res = await fetch(`${BASE}/${roomId}/invitations/accept`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}
