import { apiFetch } from './http';

export const sendFriendRequest = (toUsername, message = '') =>
  apiFetch('/contacts/requests', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ toUsername, message }) });

export const getIncomingRequests = () => apiFetch('/contacts/requests/incoming');

export const acceptRequest = (id) => apiFetch(`/contacts/requests/${id}/accept`, { method: 'POST' });

export const rejectRequest = (id) => apiFetch(`/contacts/requests/${id}/reject`, { method: 'POST' });

export const getFriends = () => apiFetch('/contacts');

export const removeFriend = (userId) => apiFetch(`/contacts/${userId}`, { method: 'DELETE' });

export const banUser = (userId) => apiFetch(`/contacts/${userId}/ban`, { method: 'POST' });

export const unbanUser = (userId) => apiFetch(`/contacts/${userId}/ban`, { method: 'DELETE' });
