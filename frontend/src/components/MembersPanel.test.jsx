import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MembersPanel } from './MembersPanel';
import * as rooms from '../api/rooms';
import * as contacts from '../api/contacts';

vi.mock('../api/rooms');
vi.mock('../api/contacts');

const mockMembers = [
  { userId: 1, username: 'alice', role: 'OWNER' },
  { userId: 2, username: 'bob', role: 'MEMBER' },
  { userId: 3, username: 'carol', role: 'ADMIN' },
];

describe('MembersPanel', () => {
  beforeEach(() => {
    rooms.listMembers.mockResolvedValue(mockMembers);
    contacts.sendFriendRequest.mockResolvedValue({});
  });

  it('renders all members', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={true} />);
    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByText('bob')).toBeInTheDocument();
    expect(screen.getByText('carol')).toBeInTheDocument();
  });

  it('shows member count in header', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={false} />);
    await waitFor(() => expect(screen.getByText('Members (3)')).toBeInTheDocument());
  });

  it('shows ONLINE presence for members in presenceMap', async () => {
    render(
      <MembersPanel
        roomId={1}
        currentUserId={1}
        isAdminOrOwner={false}
        presenceMap={{ 1: 'ONLINE', 2: 'AFK' }}
      />
    );
    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByLabelText('Online')).toBeInTheDocument();
    expect(screen.getByLabelText('Away')).toBeInTheDocument();
  });

  it('defaults to OFFLINE for members not in presenceMap', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={false} presenceMap={{}} />);
    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    const offlineIndicators = screen.getAllByLabelText('Offline');
    expect(offlineIndicators).toHaveLength(3);
  });

  it('shows Add Friend button for non-self members', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={false} />);
    await waitFor(() => expect(screen.getByText('bob')).toBeInTheDocument());
    const addBtns = screen.getAllByTitle('Add friend');
    expect(addBtns).toHaveLength(2); // bob and carol, not alice (self)
  });

  it('marks Add Friend button as sent after click', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={false} />);
    await waitFor(() => expect(screen.getByText('bob')).toBeInTheDocument());
    const addBtns = screen.getAllByTitle('Add friend');
    fireEvent.click(addBtns[0]);
    await waitFor(() => expect(contacts.sendFriendRequest).toHaveBeenCalled());
    expect(screen.getByTitle('Request sent')).toBeInTheDocument();
  });

  it('shows role labels', async () => {
    render(<MembersPanel roomId={1} currentUserId={1} isAdminOrOwner={false} />);
    await waitFor(() => expect(screen.getByText('OWNER')).toBeInTheDocument());
    expect(screen.getByText('MEMBER')).toBeInTheDocument();
    expect(screen.getByText('ADMIN')).toBeInTheDocument();
  });
});
