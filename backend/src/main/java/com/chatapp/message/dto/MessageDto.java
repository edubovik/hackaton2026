package com.chatapp.message.dto;

import com.chatapp.attachment.dto.AttachmentDto;
import com.chatapp.message.entity.Message;

import java.time.OffsetDateTime;
import java.util.List;

public record MessageDto(
        Long id,
        Long roomId,
        Long senderId,
        String senderUsername,
        Long recipientId,
        Long replyToId,
        String content,
        boolean edited,
        boolean deleted,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AttachmentDto> attachments
) {
    public static MessageDto from(Message m) {
        return from(m, List.of());
    }

    public static MessageDto from(Message m, List<AttachmentDto> attachments) {
        return new MessageDto(
                m.getId(),
                m.getRoomId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.getRecipientId(),
                m.getReplyToId(),
                m.isDeleted() ? "This message was deleted" : m.getContent(),
                m.isEdited(),
                m.isDeleted(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                attachments
        );
    }
}
