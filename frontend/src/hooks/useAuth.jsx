import { createContext, useContext, useState, useEffect } from 'react';
import { login as apiLogin, logout as apiLogout, register as apiRegister, getMe } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [authChecked, setAuthChecked] = useState(false);

  // On mount, validate session via cookies — works across tabs without re-login
  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => {})
      .finally(() => setAuthChecked(true));
  }, []);

  async function login(email, password, keepMeSignedIn) {
    await apiLogin(email, password, keepMeSignedIn);
    const profile = await getMe();
    setUser(profile);
  }

  async function register(email, username, password) {
    await apiRegister(email, username, password);
  }

  async function logout() {
    await apiLogout().catch(() => {});
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, authChecked, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
