const ACCESS_KEY = 'auth_access';
const REFRESH_KEY = 'auth_refresh';

export function storeTokens(accessToken, refreshToken, persist) {
  if (accessToken) sessionStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) {
    if (persist) localStorage.setItem(REFRESH_KEY, refreshToken);
    else sessionStorage.setItem(REFRESH_KEY, refreshToken);
  }
}

export function getAccessToken() {
  return sessionStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_KEY) || localStorage.getItem(REFRESH_KEY);
}

// Ends only this tab's session; leaves the localStorage persistent token intact
// so other tabs (and browser-restart bootstrap) are unaffected.
export function clearSessionTokens() {
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

// Clears everything — used when a refresh call itself fails (token revoked/expired).
export function clearTokens() {
  clearSessionTokens();
  localStorage.removeItem(REFRESH_KEY);
}
