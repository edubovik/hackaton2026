const BASE = '/api/v1';

async function request(method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.error || `Request failed: ${res.status}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const register = (email, username, password) =>
  request('POST', '/auth/register', { email, username, password });

export const login = (email, password, keepMeSignedIn) =>
  request('POST', '/auth/login', { email, password, keepMeSignedIn });

export const logout = () => request('POST', '/auth/logout');

export const refresh = () => request('POST', '/auth/refresh');

export const getSessions = () => request('GET', '/auth/sessions');

export const deleteSession = (id) => request('DELETE', `/auth/sessions/${id}`);

export const changePassword = (currentPassword, newPassword) =>
  request('POST', '/users/me/password', { currentPassword, newPassword });

export const deleteAccount = (password) =>
  request('DELETE', '/users/me', { password });
