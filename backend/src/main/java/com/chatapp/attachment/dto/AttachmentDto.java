package com.chatapp.attachment.dto;

import com.chatapp.attachment.entity.Attachment;

import java.time.OffsetDateTime;

public record AttachmentDto(
        Long id,
        Long messageId,
        Long uploaderId,
        String filename,
        String contentType,
        long sizeBytes,
        String comment,
        OffsetDateTime createdAt
) {
    public static AttachmentDto from(Attachment a) {
        return new AttachmentDto(
                a.getId(),
                a.getMessage().getId(),
                a.getUploader().getId(),
                a.getFilename(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getComment(),
                a.getCreatedAt()
        );
    }
}
