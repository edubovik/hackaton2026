import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ManageRoomModal } from './ManageRoomModal';
import * as rooms from '../api/rooms';

vi.mock('../api/rooms');

const room = { id: 1, name: 'General', description: '', isPublic: true };

describe('ManageRoomModal', () => {
  beforeEach(() => {
    rooms.listMembers.mockResolvedValue([
      { userId: 1, username: 'owner', role: 'OWNER', joinedAt: '' },
      { userId: 2, username: 'alice', role: 'MEMBER', joinedAt: '' },
    ]);
    rooms.listBans.mockResolvedValue([]);
  });

  it('renders Members tab by default and shows member list', async () => {
    render(
      <ManageRoomModal room={room} currentUserId={1} isOwner onClose={() => {}} onDeleted={() => {}} />
    );
    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByText('Members')).toBeInTheDocument();
  });

  it('switches to Banned tab and shows empty state', async () => {
    render(
      <ManageRoomModal room={room} currentUserId={1} isOwner onClose={() => {}} onDeleted={() => {}} />
    );
    await waitFor(() => screen.getByText('alice'));

    await userEvent.click(screen.getByRole('button', { name: 'Banned' }));
    expect(screen.getByText('No bans')).toBeInTheDocument();
  });

  it('switches to Admins tab and shows admin', async () => {
    render(
      <ManageRoomModal room={room} currentUserId={1} isOwner onClose={() => {}} onDeleted={() => {}} />
    );
    await waitFor(() => screen.getByText('alice'));

    await userEvent.click(screen.getByRole('button', { name: 'Admins' }));
    expect(screen.getAllByText(/owner/).length).toBeGreaterThan(0);
  });

  it('shows Settings tab only for owner', async () => {
    render(
      <ManageRoomModal room={room} currentUserId={1} isOwner onClose={() => {}} onDeleted={() => {}} />
    );
    await waitFor(() => screen.getByText('alice'));

    await userEvent.click(screen.getByRole('button', { name: 'Settings' }));
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
  });
});
