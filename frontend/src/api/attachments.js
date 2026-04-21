const BASE = '/api/v1';

export async function uploadAttachment({ file, roomId, recipientId, content, replyToId }) {
  const form = new FormData();
  form.append('file', file);
  if (roomId != null) form.append('roomId', String(roomId));
  if (recipientId != null) form.append('recipientId', String(recipientId));
  if (content) form.append('content', content);
  if (replyToId != null) form.append('replyToId', String(replyToId));

  let res = await fetch(`${BASE}/attachments`, { method: 'POST', credentials: 'include', body: form });

  if (res.status === 401) {
    await fetch(`${BASE}/auth/refresh`, { method: 'POST', credentials: 'include' }).catch(() => {});
    res = await fetch(`${BASE}/attachments`, { method: 'POST', credentials: 'include', body: form });
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || 'Upload failed');
  }
  return res.json();
}

export function attachmentDownloadUrl(id) {
  return `${BASE}/attachments/${id}`;
}
