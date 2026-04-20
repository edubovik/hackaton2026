import { useState, useRef } from 'react';
import { ReplyBar } from './ReplyBar';
import styles from './MessageComposer.module.css';

export function MessageComposer({ onSend, replyTo, onCancelReply, disabled }) {
  const [text, setText] = useState('');
  const textareaRef = useRef(null);

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  }

  function submit() {
    const content = text.trim();
    if (!content || disabled) return;
    onSend(content, replyTo?.id ?? null);
    setText('');
    onCancelReply();
  }

  return (
    <div className={styles.wrapper}>
      <ReplyBar replyTo={replyTo} onCancel={onCancelReply} />
      <div className={styles.row}>
        <textarea
          ref={textareaRef}
          className={styles.input}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Type a message… (Enter to send, Shift+Enter for newline)"
          rows={1}
          disabled={disabled}
        />
        <button
          className={styles.sendBtn}
          onClick={submit}
          disabled={!text.trim() || disabled}
          aria-label="Send"
        >
          Send
        </button>
      </div>
    </div>
  );
}
