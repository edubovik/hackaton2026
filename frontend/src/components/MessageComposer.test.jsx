import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MessageComposer } from './MessageComposer';

describe('MessageComposer', () => {
  it('calls onSend with content when Enter is pressed', () => {
    const onSend = vi.fn();
    render(
      <MessageComposer
        onSend={onSend}
        replyTo={null}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    const textarea = screen.getByPlaceholderText(/Type a message/);
    fireEvent.change(textarea, { target: { value: 'Hello there' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });

    expect(onSend).toHaveBeenCalledWith('Hello there', null);
  });

  it('does not submit on Shift+Enter', () => {
    const onSend = vi.fn();
    render(
      <MessageComposer
        onSend={onSend}
        replyTo={null}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    const textarea = screen.getByPlaceholderText(/Type a message/);
    fireEvent.change(textarea, { target: { value: 'line1' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: true });

    expect(onSend).not.toHaveBeenCalled();
  });

  it('shows reply bar when replyTo is set', () => {
    const replyTo = { id: 5, senderUsername: 'bob', content: 'Original message' };
    render(
      <MessageComposer
        onSend={() => {}}
        replyTo={replyTo}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    expect(screen.getByText(/Replying to/)).toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
  });

  it('does not send blank message', () => {
    const onSend = vi.fn();
    render(
      <MessageComposer
        onSend={onSend}
        replyTo={null}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    const textarea = screen.getByPlaceholderText(/Type a message/);
    fireEvent.change(textarea, { target: { value: '   ' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });

    expect(onSend).not.toHaveBeenCalled();
  });

  it('accepts dropped file and shows pending file name', () => {
    render(
      <MessageComposer
        onSend={() => {}}
        onSendAttachment={() => {}}
        replyTo={null}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    const wrapper = screen.getByPlaceholderText(/Type a message/).closest('div').parentElement;
    const file = new File(['hello'], 'test.txt', { type: 'text/plain' });
    fireEvent.dragOver(wrapper, { dataTransfer: { files: [file] } });
    fireEvent.drop(wrapper, { dataTransfer: { files: [file] } });

    expect(screen.getByText(/test\.txt/)).toBeInTheDocument();
  });

  it('passes replyToId to onSend', () => {
    const onSend = vi.fn();
    const replyTo = { id: 42, senderUsername: 'carol', content: 'Quoted msg' };
    render(
      <MessageComposer
        onSend={onSend}
        replyTo={replyTo}
        onCancelReply={() => {}}
        disabled={false}
      />
    );

    const textarea = screen.getByPlaceholderText(/Type a message/);
    fireEvent.change(textarea, { target: { value: 'My reply' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });

    expect(onSend).toHaveBeenCalledWith('My reply', 42);
  });
});
