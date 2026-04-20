import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import ResetPasswordPage from './ResetPasswordPage';
import * as authApi from '../api/auth';

function renderWithToken(token = 'valid-token') {
  return render(
    <MemoryRouter initialEntries={[`/reset-password?token=${token}`]}>
      <Routes>
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/signin" element={<div>Sign In Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ResetPasswordPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('renders new password and confirm password fields', () => {
    renderWithToken();
    expect(screen.getByLabelText(/new password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  it('shows error when passwords do not match', async () => {
    renderWithToken();
    await userEvent.type(screen.getByLabelText(/new password/i), 'password1');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'password2');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it('shows error when fields are empty', async () => {
    renderWithToken();
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(screen.getByText(/both fields are required/i)).toBeInTheDocument();
  });

  it('redirects to signin on success', async () => {
    vi.spyOn(authApi, 'resetPassword').mockResolvedValue(null);
    renderWithToken();
    await userEvent.type(screen.getByLabelText(/new password/i), 'newpassword');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'newpassword');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/sign in page/i)).toBeInTheDocument();
  });

  it('shows error on API failure', async () => {
    vi.spyOn(authApi, 'resetPassword').mockRejectedValue(new Error('Token has expired'));
    renderWithToken();
    await userEvent.type(screen.getByLabelText(/new password/i), 'newpassword');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'newpassword');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/token has expired/i)).toBeInTheDocument();
  });
});
