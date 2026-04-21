let refreshPromise = null;

async function doFetch(url, options) {
  return fetch(url, { credentials: 'include', ...options });
}

export async function apiFetch(path, options = {}) {
  const res = await doFetch(`/api/v1${path}`, options);

  if (res.status === 401) {
    if (!refreshPromise) {
      refreshPromise = doFetch('/api/v1/auth/refresh', { method: 'POST' })
        .finally(() => { refreshPromise = null; });
    }
    const refreshRes = await refreshPromise;
    if (!refreshRes.ok) {
      window.location.replace('/signin');
      throw new Error('Session expired');
    }
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
