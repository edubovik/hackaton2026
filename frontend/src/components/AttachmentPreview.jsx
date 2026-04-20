import { attachmentDownloadUrl } from '../api/attachments';
import styles from './AttachmentPreview.module.css';

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function AttachmentPreview({ attachment }) {
  const isImage = attachment.contentType?.startsWith('image/');
  const url = attachmentDownloadUrl(attachment.id);

  return (
    <div className={styles.wrapper}>
      {isImage ? (
        <a href={url} target="_blank" rel="noreferrer">
          <img
            className={styles.thumbnail}
            src={url}
            alt={attachment.filename}
          />
        </a>
      ) : (
        <a className={styles.fileLink} href={url} download={attachment.filename}>
          <span className={styles.fileIcon}>📎</span>
          <span className={styles.fileName}>{attachment.filename}</span>
        </a>
      )}
      <span className={styles.meta}>
        {attachment.filename} · {formatBytes(attachment.sizeBytes)}
      </span>
    </div>
  );
}
