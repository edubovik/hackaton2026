import { apiFetch } from './http';

export const register = (email, username, password) =>
  apiFetch('/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, username, password }) });

export const login = (email, password, keepMeSignedIn) =>
  apiFetch('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password, keepMeSignedIn }) });

export const logout = () =>
  apiFetch('/auth/logout', { method: 'POST' });

export const refresh = () =>
  apiFetch('/auth/refresh', { method: 'POST' });

export const getSessions = () => apiFetch('/auth/sessions');

export const deleteSession = (id) => apiFetch(`/auth/sessions/${id}`, { method: 'DELETE' });

export const changePassword = (currentPassword, newPassword) =>
  apiFetch('/users/me/password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ currentPassword, newPassword }) });

export const deleteAccount = (password) =>
  apiFetch('/users/me', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password }) });

export const getMe = () => apiFetch('/users/me');

export const forgotPassword = (email) =>
  apiFetch('/auth/forgot-password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) });

export const resetPassword = (token, newPassword) =>
  apiFetch('/auth/reset-password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ token, newPassword }) });
