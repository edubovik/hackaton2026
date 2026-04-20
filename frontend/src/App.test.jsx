import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import App from './App';
import * as AuthContext from './hooks/useAuth';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual };
});

test('renders sign in page at /signin route', () => {
  vi.spyOn(AuthContext, 'useAuth').mockReturnValue({ user: null, login: vi.fn(), logout: vi.fn(), register: vi.fn() });
  render(<App />);
  // App redirects unauthenticated users to /signin
  expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
});
