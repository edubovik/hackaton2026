import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MessageList } from './MessageList';

const noop = () => {};

function makeMsg(overrides = {}) {
  return {
    id: 1,
    senderId: 10,
    senderUsername: 'alice',
    content: 'Hello world',
    edited: false,
    deleted: false,
    replyToId: null,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

describe('MessageList', () => {
  it('renders messages', () => {
    const messages = [
      makeMsg({ id: 1, content: 'First message' }),
      makeMsg({ id: 2, content: 'Second message' }),
    ];

    render(
      <MessageList
        messages={messages}
        hasMore={false}
        loading={false}
        onLoadMore={noop}
        currentUserId={10}
        isRoomAdmin={false}
        onReply={noop}
        onMessageUpdated={noop}
      />
    );

    expect(screen.getByText('First message')).toBeInTheDocument();
    expect(screen.getByText('Second message')).toBeInTheDocument();
  });

  it('shows edited indicator on edited messages', () => {
    const messages = [makeMsg({ id: 1, content: 'Edited text', edited: true })];

    render(
      <MessageList
        messages={messages}
        hasMore={false}
        loading={false}
        onLoadMore={noop}
        currentUserId={10}
        isRoomAdmin={false}
        onReply={noop}
        onMessageUpdated={noop}
      />
    );

    expect(screen.getByText('(edited)')).toBeInTheDocument();
  });

  it('shows deleted placeholder for deleted messages', () => {
    const messages = [makeMsg({ id: 1, content: 'This message was deleted', deleted: true })];

    render(
      <MessageList
        messages={messages}
        hasMore={false}
        loading={false}
        onLoadMore={noop}
        currentUserId={10}
        isRoomAdmin={false}
        onReply={noop}
        onMessageUpdated={noop}
      />
    );

    expect(screen.getByText('This message was deleted')).toBeInTheDocument();
  });

  it('shows loading indicator when loading is true', () => {
    render(
      <MessageList
        messages={[]}
        hasMore={true}
        loading={true}
        onLoadMore={noop}
        currentUserId={10}
        isRoomAdmin={false}
        onReply={noop}
        onMessageUpdated={noop}
      />
    );

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });
});
