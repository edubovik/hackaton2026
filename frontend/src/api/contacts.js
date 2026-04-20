const BASE = '/api/v1/contacts';

export async function sendFriendRequest(toUsername, message = '') {
  const res = await fetch(`${BASE}/requests`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ toUsername, message }),
  });
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function getIncomingRequests() {
  const res = await fetch(`${BASE}/requests/incoming`);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function acceptRequest(id) {
  const res = await fetch(`${BASE}/requests/${id}/accept`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function rejectRequest(id) {
  const res = await fetch(`${BASE}/requests/${id}/reject`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function getFriends() {
  const res = await fetch(BASE);
  if (!res.ok) throw new Error((await res.json()).error);
  return res.json();
}

export async function removeFriend(userId) {
  const res = await fetch(`${BASE}/${userId}`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function banUser(userId) {
  const res = await fetch(`${BASE}/${userId}/ban`, { method: 'POST' });
  if (!res.ok) throw new Error((await res.json()).error);
}

export async function unbanUser(userId) {
  const res = await fetch(`${BASE}/${userId}/ban`, { method: 'DELETE' });
  if (!res.ok) throw new Error((await res.json()).error);
}
