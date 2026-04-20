import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import SignInPage from './SignInPage';
import * as AuthContext from '../hooks/useAuth';

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <SignInPage />
    </MemoryRouter>
  );
}

describe('SignInPage', () => {
  beforeEach(() => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      login: vi.fn(),
      user: null,
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it('renders email and password fields', () => {
    renderWithRouter();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('renders keep me signed in checkbox', () => {
    renderWithRouter();
    expect(screen.getByRole('checkbox', { name: /keep me signed in/i })).toBeInTheDocument();
  });

  it('shows error when submitting empty form', async () => {
    renderWithRouter();
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(screen.getByText(/email and password are required/i)).toBeInTheDocument();
  });

  it('shows error message on failed login', async () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      login: vi.fn().mockRejectedValue(new Error('Invalid credentials')),
      user: null,
    });
    renderWithRouter();
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    expect(await screen.findByText(/invalid credentials/i)).toBeInTheDocument();
  });
});
