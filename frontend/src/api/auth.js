import { apiFetch } from './http';
import { storeTokens, getRefreshToken, clearSessionTokens } from './tokenStorage';

export const register = (email, username, password) =>
  apiFetch('/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, username, password }) });

export const login = async (email, password, keepMeSignedIn) => {
  const data = await apiFetch('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password, keepMeSignedIn }) });
  storeTokens(data.accessToken, data.refreshToken, keepMeSignedIn);
};

export const logout = async () => {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    await apiFetch('/auth/logout', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken }) }).catch(() => {});
  }
  clearSessionTokens();
};

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
