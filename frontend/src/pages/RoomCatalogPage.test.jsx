import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RoomCatalogPage from './RoomCatalogPage';
import * as rooms from '../api/rooms';

vi.mock('../api/rooms');

const emptyPage = { content: [], totalPages: 0 };

describe('RoomCatalogPage', () => {
  beforeEach(() => {
    rooms.listRooms.mockResolvedValue(emptyPage);
  });

  it('renders catalog title and search input', async () => {
    render(<RoomCatalogPage />);
    expect(screen.getByText('Room Catalog')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Search rooms…')).toBeInTheDocument();
  });

  it('shows empty state when no rooms', async () => {
    render(<RoomCatalogPage />);
    await waitFor(() => expect(screen.getByText('No rooms found')).toBeInTheDocument());
  });

  it('renders rooms returned from API', async () => {
    rooms.listRooms.mockResolvedValue({
      content: [
        { id: 1, name: 'General', description: 'Chat here', isPublic: true },
        { id: 2, name: 'Random', description: '', isPublic: true },
      ],
      totalPages: 1,
    });
    render(<RoomCatalogPage />);
    await waitFor(() => expect(screen.getByText('General')).toBeInTheDocument());
    expect(screen.getByText('Random')).toBeInTheDocument();
  });

  it('filters results when search input changes', async () => {
    render(<RoomCatalogPage />);
    const input = screen.getByPlaceholderText('Search rooms…');
    await userEvent.type(input, 'gen');

    await waitFor(() =>
      expect(rooms.listRooms).toHaveBeenCalledWith('gen', 0)
    );
  });

  it('opens create room modal on button click', async () => {
    render(<RoomCatalogPage />);
    await userEvent.click(screen.getByRole('button', { name: '+ Create Room' }));
    expect(screen.getByText('Create Room')).toBeInTheDocument();
  });
});
