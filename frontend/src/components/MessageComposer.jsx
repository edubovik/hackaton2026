import { useState, useRef } from 'react';
import { ReplyBar } from './ReplyBar';
import { AttachmentUploader } from './AttachmentUploader';
import styles from './MessageComposer.module.css';

const MAX_FILE_MB = 20;

export function MessageComposer({ onSend, onSendAttachment, replyTo, onCancelReply, disabled }) {
  const [text, setText] = useState('');
  const [pendingFile, setPendingFile] = useState(null);
  const [uploadError, setUploadError] = useState('');
  const [dragging, setDragging] = useState(false);
  const textareaRef = useRef(null);

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  }

  function submit() {
    if (disabled) return;
    if (pendingFile) {
      onSendAttachment(pendingFile, text.trim() || null, replyTo?.id ?? null);
      setPendingFile(null);
      setText('');
      onCancelReply();
      return;
    }
    const content = text.trim();
    if (!content) return;
    onSend(content, replyTo?.id ?? null);
    setText('');
    onCancelReply();
  }

  function handleFileSelected(file) {
    if (file.size > MAX_FILE_MB * 1024 * 1024) {
      setUploadError(`File too large — max ${MAX_FILE_MB} MB`);
      return;
    }
    setUploadError('');
    setPendingFile(file);
    textareaRef.current?.focus();
  }

  function removePending() {
    setPendingFile(null);
    setUploadError('');
  }

  function handleDragOver(e) {
    e.preventDefault();
    setDragging(true);
  }

  function handleDragLeave(e) {
    if (!e.currentTarget.contains(e.relatedTarget)) setDragging(false);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelected(file);
  }

  const canSend = !disabled && (!!text.trim() || !!pendingFile);

  return (
    <div
      className={`${styles.wrapper} ${dragging ? styles.dragging : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <ReplyBar replyTo={replyTo} onCancel={onCancelReply} />
      {pendingFile && (
        <div className={styles.pendingFile}>
          <span>📎 {pendingFile.name}</span>
          <button type="button" className={styles.removeFile} onClick={removePending} aria-label="Remove file">✕</button>
        </div>
      )}
      {uploadError && <div className={styles.error}>{uploadError}</div>}
      <div className={styles.row}>
        <AttachmentUploader
          onFileSelected={handleFileSelected}
          onError={(msg) => setUploadError(msg)}
        />
        <textarea
          ref={textareaRef}
          className={styles.input}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={pendingFile ? 'Add a comment… (optional)' : 'Type a message… (Enter to send, Shift+Enter for newline)'}
          rows={1}
          disabled={disabled}
        />
        <button
          className={styles.sendBtn}
          onClick={submit}
          disabled={!canSend}
          aria-label="Send"
        >
          Send
        </button>
      </div>
    </div>
  );
}
