const BASE = '/api/v1';

export async function fetchRoomHistory(roomId, before) {
  const params = before ? `?before=${before}` : '';
  const res = await fetch(`${BASE}/rooms/${roomId}/messages${params}`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to load room history');
  return res.json();
}

export async function fetchDmHistory(partnerId, before) {
  const params = before ? `?before=${before}` : '';
  const res = await fetch(`${BASE}/messages/dm/${partnerId}${params}`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to load DM history');
  return res.json();
}

export async function editMessage(id, content) {
  const res = await fetch(`${BASE}/messages/${id}`, {
    method: 'PATCH',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  });
  if (!res.ok) throw new Error('Failed to edit message');
  return res.json();
}

export async function deleteMessage(id) {
  const res = await fetch(`${BASE}/messages/${id}`, {
    method: 'DELETE',
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to delete message');
}

export async function markRoomRead(roomId) {
  await fetch(`${BASE}/rooms/${roomId}/messages/read`, {
    method: 'POST',
    credentials: 'include',
  });
}

export async function markDmRead(partnerId) {
  await fetch(`${BASE}/messages/dm/${partnerId}/read`, {
    method: 'POST',
    credentials: 'include',
  });
}

export async function fetchUnreadCounts() {
  const res = await fetch(`${BASE}/messages/unread`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to load unread counts');
  return res.json();
}
