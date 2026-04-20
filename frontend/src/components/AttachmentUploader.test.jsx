import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AttachmentUploader } from './AttachmentUploader';

describe('AttachmentUploader', () => {
  let onFileSelected;
  let onError;

  beforeEach(() => {
    onFileSelected = vi.fn();
    onError = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders attach button', () => {
    render(<AttachmentUploader onFileSelected={onFileSelected} onError={onError} />);
    expect(screen.getByRole('button', { name: /attach file/i })).toBeInTheDocument();
  });

  it('clicking button opens file input', () => {
    render(<AttachmentUploader onFileSelected={onFileSelected} onError={onError} />);
    const input = screen.getByTestId('file-input');
    const clickSpy = vi.spyOn(input, 'click');
    fireEvent.click(screen.getByRole('button', { name: /attach file/i }));
    expect(clickSpy).toHaveBeenCalled();
  });

  it('selecting a valid file calls onFileSelected', () => {
    render(<AttachmentUploader onFileSelected={onFileSelected} onError={onError} />);
    const input = screen.getByTestId('file-input');
    const file = new File(['hello'], 'hello.txt', { type: 'text/plain' });
    fireEvent.change(input, { target: { files: [file] } });
    expect(onFileSelected).toHaveBeenCalledWith(file);
    expect(onError).not.toHaveBeenCalled();
  });

  it('selecting an oversized image calls onError', () => {
    render(<AttachmentUploader onFileSelected={onFileSelected} onError={onError} />);
    const input = screen.getByTestId('file-input');
    const bigContent = new Uint8Array(4 * 1024 * 1024);
    const file = new File([bigContent], 'big.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [file] } });
    expect(onError).toHaveBeenCalledWith(expect.stringContaining('3 MB'));
    expect(onFileSelected).not.toHaveBeenCalled();
  });

  it('paste event with file calls onFileSelected', () => {
    render(<AttachmentUploader onFileSelected={onFileSelected} onError={onError} />);
    const file = new File(['data'], 'pasted.png', { type: 'image/png' });
    const pasteEvent = new Event('paste');
    Object.defineProperty(pasteEvent, 'clipboardData', {
      value: {
        items: [{ kind: 'file', getAsFile: () => file }],
      },
    });
    window.dispatchEvent(pasteEvent);
    expect(onFileSelected).toHaveBeenCalledWith(file);
  });
});
