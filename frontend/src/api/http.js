import { getAccessToken, getRefreshToken, storeTokens, clearTokens } from './tokenStorage';

let refreshPromise = null;

async function doFetch(url, options = {}) {
  const token = getAccessToken();
  const headers = { ...(options.headers || {}) };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return fetch(url, { credentials: 'include', ...options, headers });
}

export async function apiFetch(path, options = {}) {
  const res = await doFetch(`/api/v1${path}`, options);

  if (res.status === 401) {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
      throw new Error('Session expired');
    }

    if (!refreshPromise) {
      refreshPromise = fetch('/api/v1/auth/refresh', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      }).finally(() => { refreshPromise = null; });
    }
    const refreshRes = await refreshPromise;
    if (!refreshRes.ok) {
      clearTokens();
      if (window.location.pathname !== '/signin') window.location.replace('/signin');
      throw new Error('Session expired');
    }
    const { accessToken } = await refreshRes.json();
    storeTokens(accessToken, null, false);

    const retry = await doFetch(`/api/v1${path}`, options);
    if (!retry.ok) {
      const data = await retry.json().catch(() => ({}));
      throw new Error(data.error || `Request failed: ${retry.status}`);
    }
    const text = await retry.text();
    return text ? JSON.parse(text) : null;
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.error || `Request failed: ${res.status}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}
