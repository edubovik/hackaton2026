import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import ForgotPasswordPage from './ForgotPasswordPage';
import * as authApi from '../api/auth';

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <ForgotPasswordPage />
    </MemoryRouter>
  );
}

describe('ForgotPasswordPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders email field and submit button', () => {
    renderWithRouter();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument();
  });

  it('shows error when submitting empty form', async () => {
    renderWithRouter();
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(screen.getByText(/email is required/i)).toBeInTheDocument();
  });

  it('shows success message after successful submit', async () => {
    vi.spyOn(authApi, 'forgotPassword').mockResolvedValue(null);
    renderWithRouter();
    await userEvent.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/reset link has been sent/i)).toBeInTheDocument();
  });

  it('shows error on API failure', async () => {
    vi.spyOn(authApi, 'forgotPassword').mockRejectedValue(new Error('Network error'));
    renderWithRouter();
    await userEvent.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/something went wrong/i)).toBeInTheDocument();
  });
});
