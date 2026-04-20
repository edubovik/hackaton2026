import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { NavBar } from './NavBar';

const mockLogout = vi.fn();
const mockNavigate = vi.fn();

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ user: { id: 1, username: 'alice' }, logout: mockLogout }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('./ChangePasswordModal', () => ({
  default: ({ onClose }) => (
    <div role="dialog" aria-label="change password">
      <button onClick={onClose}>Close</button>
    </div>
  ),
}));

vi.mock('./DeleteAccountModal', () => ({
  default: ({ onClose }) => (
    <div role="dialog" aria-label="delete account">
      <button onClick={onClose}>Close</button>
    </div>
  ),
}));

function renderNavBar() {
  return render(
    <MemoryRouter>
      <NavBar />
    </MemoryRouter>
  );
}

describe('NavBar', () => {
  beforeEach(() => {
    mockLogout.mockResolvedValue(undefined);
    mockNavigate.mockClear();
  });

  it('renders logo and nav links', () => {
    renderNavBar();
    expect(screen.getByText('ChatApp')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /rooms/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /contacts/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sessions/i })).toBeInTheDocument();
  });

  it('shows username in profile button', () => {
    renderNavBar();
    expect(screen.getByRole('button', { name: /profile menu/i })).toHaveTextContent('alice');
  });

  it('opens dropdown when profile button clicked', () => {
    renderNavBar();
    fireEvent.click(screen.getByRole('button', { name: /profile menu/i }));
    expect(screen.getByRole('menuitem', { name: /change password/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /delete account/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /sign out/i })).toBeInTheDocument();
  });

  it('closes dropdown when clicking outside', async () => {
    renderNavBar();
    fireEvent.click(screen.getByRole('button', { name: /profile menu/i }));
    expect(screen.getByRole('menu')).toBeInTheDocument();
    fireEvent.mouseDown(document.body);
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('opens ChangePasswordModal from dropdown', () => {
    renderNavBar();
    fireEvent.click(screen.getByRole('button', { name: /profile menu/i }));
    fireEvent.click(screen.getByRole('menuitem', { name: /change password/i }));
    expect(screen.getByRole('dialog', { name: /change password/i })).toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('opens DeleteAccountModal from dropdown', () => {
    renderNavBar();
    fireEvent.click(screen.getByRole('button', { name: /profile menu/i }));
    fireEvent.click(screen.getByRole('menuitem', { name: /delete account/i }));
    expect(screen.getByRole('dialog', { name: /delete account/i })).toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('calls logout and navigates to /signin on sign out', async () => {
    renderNavBar();
    fireEvent.click(screen.getByRole('button', { name: /profile menu/i }));
    fireEvent.click(screen.getByRole('menuitem', { name: /sign out/i }));
    await waitFor(() => expect(mockLogout).toHaveBeenCalled());
    expect(mockNavigate).toHaveBeenCalledWith('/signin');
  });
});
