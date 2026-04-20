import { useRef, useEffect } from 'react';
import styles from './AttachmentUploader.module.css';

const IMAGE_LIMIT = 3 * 1024 * 1024;
const FILE_LIMIT = 20 * 1024 * 1024;

function validate(file) {
  const isImage = file.type.startsWith('image/');
  const limit = isImage ? IMAGE_LIMIT : FILE_LIMIT;
  if (file.size > limit) {
    return isImage ? 'Image exceeds 3 MB limit' : 'File exceeds 20 MB limit';
  }
  return null;
}

export function AttachmentUploader({ onFileSelected, onError }) {
  const inputRef = useRef(null);

  useEffect(() => {
    function handlePaste(e) {
      const items = e.clipboardData?.items;
      if (!items) return;
      for (const item of items) {
        if (item.kind === 'file') {
          const file = item.getAsFile();
          if (!file) continue;
          const error = validate(file);
          if (error) { onError(error); return; }
          onFileSelected(file);
          return;
        }
      }
    }
    window.addEventListener('paste', handlePaste);
    return () => window.removeEventListener('paste', handlePaste);
  }, [onFileSelected, onError]);

  function handleChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    const error = validate(file);
    if (error) { onError(error); e.target.value = ''; return; }
    onFileSelected(file);
    e.target.value = '';
  }

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        className={styles.hiddenInput}
        onChange={handleChange}
        aria-label="Attach file"
        data-testid="file-input"
      />
      <button
        type="button"
        className={styles.attachBtn}
        onClick={() => inputRef.current?.click()}
        aria-label="Attach file"
        title="Attach file"
      >
        📎
      </button>
    </>
  );
}
