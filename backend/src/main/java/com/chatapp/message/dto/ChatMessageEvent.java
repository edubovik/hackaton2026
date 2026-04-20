package com.chatapp.message.dto;

public record ChatMessageEvent(
        String type,       // "ROOM" or "DM"
        Long roomId,
        Long senderId,
        String senderUsername,
        Long recipientId,
        String content,
        Long replyToId
) {}
