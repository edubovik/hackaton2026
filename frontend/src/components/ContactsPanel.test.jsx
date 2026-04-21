import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ContactsPanel } from './ContactsPanel';
import * as contacts from '../api/contacts';

vi.mock('../api/contacts');

describe('ContactsPanel', () => {
  beforeEach(() => {
    contacts.getFriends.mockResolvedValue([]);
    contacts.getIncomingRequests.mockResolvedValue([]);
    contacts.getBannedUsers.mockResolvedValue([]);
  });

  it('renders the friend list section', async () => {
    render(<ContactsPanel />);
    await waitFor(() => expect(screen.getByText(/Friends \(0\)/)).toBeInTheDocument());
    expect(screen.getByText('No friends yet')).toBeInTheDocument();
  });

  it('renders friends when present', async () => {
    contacts.getFriends.mockResolvedValue([
      { userId: 1, username: 'bob', presence: 'ONLINE' },
      { userId: 2, username: 'carol', presence: 'OFFLINE' },
    ]);
    render(<ContactsPanel />);

    await waitFor(() => expect(screen.getByText(/Friends \(2\)/)).toBeInTheDocument());
    expect(screen.getByText('bob')).toBeInTheDocument();
    expect(screen.getByText('carol')).toBeInTheDocument();
  });

  it('shows pending requests section when requests exist', async () => {
    contacts.getIncomingRequests.mockResolvedValue([
      { id: 10, fromUserId: 3, fromUsername: 'dave', message: 'Hi!' },
    ]);
    render(<ContactsPanel />);

    await waitFor(() => expect(screen.getByText('Pending Requests')).toBeInTheDocument());
    expect(screen.getByText('dave')).toBeInTheDocument();
    expect(screen.getByText('Hi!')).toBeInTheDocument();
  });

  it('does not show pending requests section when none exist', async () => {
    render(<ContactsPanel />);
    await waitFor(() => expect(screen.getByText(/Friends/)).toBeInTheDocument());
    expect(screen.queryByText('Pending Requests')).not.toBeInTheDocument();
  });
});
