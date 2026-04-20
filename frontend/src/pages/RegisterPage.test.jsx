import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import RegisterPage from './RegisterPage';
import * as AuthContext from '../hooks/useAuth';

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <RegisterPage />
    </MemoryRouter>
  );
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      register: vi.fn(),
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it('renders all form fields', () => {
    renderWithRouter();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  it('shows error when passwords do not match', async () => {
    renderWithRouter();
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/^username/i), 'alice');
    await userEvent.type(screen.getByLabelText(/^password/i), 'pass1');
    await userEvent.type(screen.getByLabelText(/confirm password/i), 'pass2');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it('shows error when form is empty', async () => {
    renderWithRouter();
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(screen.getByText(/all fields are required/i)).toBeInTheDocument();
  });
});
