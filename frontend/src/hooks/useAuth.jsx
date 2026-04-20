import { createContext, useContext, useState } from 'react';
import { login as apiLogin, logout as apiLogout, register as apiRegister, getMe } from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  async function login(email, password, keepMeSignedIn) {
    await apiLogin(email, password, keepMeSignedIn);
    const profile = await getMe();
    setUser(profile);
    sessionStorage.setItem('user', JSON.stringify(profile));
  }

  async function register(email, username, password) {
    await apiRegister(email, username, password);
  }

  async function logout() {
    await apiLogout().catch(() => {});
    setUser(null);
    sessionStorage.removeItem('user');
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
