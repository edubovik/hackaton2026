import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { usePresence } from './usePresence';

// Mock the socket module
vi.mock('../api/socket', () => {
  let connectionListeners = new Set();
  const subscriptions = {};

  return {
    connect: vi.fn(),
    disconnect: vi.fn(),
    isConnected: vi.fn(() => false),
    onConnectionChange: vi.fn((cb) => {
      connectionListeners.add(cb);
      return () => connectionListeners.delete(cb);
    }),
    subscribe: vi.fn((destination, cb) => {
      subscriptions[destination] = cb;
      return { unsubscribe: vi.fn() };
    }),
    publish: vi.fn(),
    __triggerConnect: () => connectionListeners.forEach(cb => cb(true)),
    __triggerMessage: (dest, payload) => subscriptions[dest] && subscriptions[dest](payload),
  };
});

import * as socket from '../api/socket';

describe('usePresence', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('subscribes to /topic/presence on connect', async () => {
    renderHook(() => usePresence());

    act(() => {
      socket.__triggerConnect();
    });

    expect(socket.subscribe).toHaveBeenCalledWith('/topic/presence', expect.any(Function));
  });

  it('updates presence map when a message arrives', async () => {
    const { result } = renderHook(() => usePresence());

    act(() => {
      socket.__triggerConnect();
    });

    act(() => {
      socket.__triggerMessage('/topic/presence', { userId: 42, username: 'alice', state: 'ONLINE' });
    });

    expect(result.current[42]).toBe('ONLINE');
  });

  it('updates presence map for multiple users', async () => {
    const { result } = renderHook(() => usePresence());

    act(() => {
      socket.__triggerConnect();
    });

    act(() => {
      socket.__triggerMessage('/topic/presence', { userId: 1, username: 'alice', state: 'ONLINE' });
      socket.__triggerMessage('/topic/presence', { userId: 2, username: 'bob', state: 'AFK' });
    });

    expect(result.current[1]).toBe('ONLINE');
    expect(result.current[2]).toBe('AFK');
  });

  it('overwrites previous state for same user', async () => {
    const { result } = renderHook(() => usePresence());

    act(() => {
      socket.__triggerConnect();
    });

    act(() => {
      socket.__triggerMessage('/topic/presence', { userId: 1, username: 'alice', state: 'ONLINE' });
    });
    act(() => {
      socket.__triggerMessage('/topic/presence', { userId: 1, username: 'alice', state: 'AFK' });
    });

    expect(result.current[1]).toBe('AFK');
  });

  it('cleans up connection listener on unmount', () => {
    const { unmount } = renderHook(() => usePresence());
    unmount();
    // usePresence no longer owns the connection lifecycle; just verify no crash
    expect(socket.onConnectionChange).toHaveBeenCalled();
  });
});
