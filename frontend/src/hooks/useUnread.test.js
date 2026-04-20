import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useUnread } from './useUnread';

const mockSubscriptions = {};
let connectionListeners = new Set();

vi.mock('../api/socket', () => ({
  isConnected: vi.fn(() => false),
  onConnectionChange: vi.fn((cb) => {
    connectionListeners.add(cb);
    return () => connectionListeners.delete(cb);
  }),
  subscribe: vi.fn((destination, cb) => {
    mockSubscriptions[destination] = cb;
    return { unsubscribe: vi.fn() };
  }),
  __triggerConnect: () => connectionListeners.forEach((cb) => cb(true)),
  __triggerMessage: (dest, payload) => mockSubscriptions[dest]?.(payload),
}));

vi.mock('../api/messages', () => ({
  fetchUnreadCounts: vi.fn().mockResolvedValue([]),
  markRoomRead: vi.fn().mockResolvedValue(undefined),
  markDmRead: vi.fn().mockResolvedValue(undefined),
}));

import * as socket from '../api/socket';
import * as messages from '../api/messages';

const rooms = [{ id: 10 }, { id: 11 }];

describe('useUnread', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    connectionListeners = new Set();
    messages.fetchUnreadCounts.mockResolvedValue([]);
  });

  it('fetches initial counts on mount', async () => {
    messages.fetchUnreadCounts.mockResolvedValue([
      { roomId: 10, count: 3 },
      { partnerId: 99, count: 1 },
    ]);
    const { result } = renderHook(() => useUnread({ rooms, userId: 1 }));

    await waitFor(() => expect(result.current.getCountForRoom(10)).toBe(3));
    expect(result.current.getCountForDm(99)).toBe(1);
  });

  it('bumps room count when message arrives on inactive room', async () => {
    const { result } = renderHook(() =>
      useUnread({ rooms, userId: 1, activeRoomId: null })
    );

    act(() => socket.__triggerConnect());
    act(() => socket.__triggerMessage('/topic/room.10', { content: 'hi' }));

    await waitFor(() => expect(result.current.getCountForRoom(10)).toBe(1));
  });

  it('does not bump count for the active room', async () => {
    const { result } = renderHook(() =>
      useUnread({ rooms, userId: 1, activeRoomId: 10 })
    );

    act(() => socket.__triggerConnect());
    act(() => socket.__triggerMessage('/topic/room.10', { content: 'hi' }));

    expect(result.current.getCountForRoom(10)).toBe(0);
  });

  it('does not subscribe to DM queue (avoids competing with useMessages)', async () => {
    renderHook(() => useUnread({ rooms, userId: 1 }));
    act(() => socket.__triggerConnect());
    const subscribedDestinations = socket.subscribe.mock.calls.map(([dest]) => dest);
    expect(subscribedDestinations).not.toContain('/queue/user.1');
  });

  it('clearRoom removes count and marks read', async () => {
    messages.fetchUnreadCounts.mockResolvedValue([{ roomId: 10, count: 5 }]);
    const { result } = renderHook(() => useUnread({ rooms, userId: 1 }));
    await waitFor(() => expect(result.current.getCountForRoom(10)).toBe(5));

    await act(async () => { await result.current.clearRoom(10); });

    expect(result.current.getCountForRoom(10)).toBe(0);
    expect(messages.markRoomRead).toHaveBeenCalledWith(10);
  });

  it('clearDm removes count and marks read', async () => {
    messages.fetchUnreadCounts.mockResolvedValue([{ partnerId: 42, count: 2 }]);
    const { result } = renderHook(() => useUnread({ rooms, userId: 1 }));
    await waitFor(() => expect(result.current.getCountForDm(42)).toBe(2));

    await act(async () => { await result.current.clearDm(42); });

    expect(result.current.getCountForDm(42)).toBe(0);
    expect(messages.markDmRead).toHaveBeenCalledWith(42);
  });

  it('accumulates multiple bumps', async () => {
    const { result } = renderHook(() =>
      useUnread({ rooms, userId: 1, activeRoomId: null })
    );

    act(() => socket.__triggerConnect());
    act(() => {
      socket.__triggerMessage('/topic/room.11', { content: 'a' });
      socket.__triggerMessage('/topic/room.11', { content: 'b' });
      socket.__triggerMessage('/topic/room.11', { content: 'c' });
    });

    await waitFor(() => expect(result.current.getCountForRoom(11)).toBe(3));
  });
});
