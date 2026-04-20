import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import SessionsPage from './SessionsPage';
import * as authApi from '../api/auth';

vi.mock('../components/NavBar', () => ({
  NavBar: () => <nav data-testid="navbar" />,
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: { id: 1, username: 'alice' }, logout: vi.fn() }),
}));

const mockSessions = [
  { id: 1, userAgent: 'Chrome/120', ipAddress: '127.0.0.1', createdAt: '2026-01-01T00:00:00Z', expiresAt: '2026-02-01T00:00:00Z' },
  { id: 2, userAgent: 'Firefox/121', ipAddress: '10.0.0.1', createdAt: '2026-01-02T00:00:00Z', expiresAt: '2026-02-02T00:00:00Z' },
];

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <SessionsPage />
    </MemoryRouter>
  );
}

describe('SessionsPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders session list', async () => {
    vi.spyOn(authApi, 'getSessions').mockResolvedValue(mockSessions);
    renderWithRouter();
    expect(await screen.findByText('Chrome/120')).toBeInTheDocument();
    expect(screen.getByText('Firefox/121')).toBeInTheDocument();
  });

  it('renders revoke button for each session', async () => {
    vi.spyOn(authApi, 'getSessions').mockResolvedValue(mockSessions);
    renderWithRouter();
    const revokeButtons = await screen.findAllByRole('button', { name: /revoke/i });
    expect(revokeButtons).toHaveLength(2);
  });

  it('removes session after revoke', async () => {
    vi.spyOn(authApi, 'getSessions').mockResolvedValue(mockSessions);
    vi.spyOn(authApi, 'deleteSession').mockResolvedValue(null);
    renderWithRouter();
    const revokeButtons = await screen.findAllByRole('button', { name: /revoke/i });
    await userEvent.click(revokeButtons[0]);
    expect(screen.queryByText('Chrome/120')).not.toBeInTheDocument();
    expect(screen.getByText('Firefox/121')).toBeInTheDocument();
  });

  it('shows empty state when no sessions', async () => {
    vi.spyOn(authApi, 'getSessions').mockResolvedValue([]);
    renderWithRouter();
    expect(await screen.findByText(/no active sessions/i)).toBeInTheDocument();
  });
});
