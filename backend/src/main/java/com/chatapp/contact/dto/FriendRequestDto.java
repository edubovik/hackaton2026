package com.chatapp.contact.dto;

import com.chatapp.contact.entity.FriendRequest;

import java.time.OffsetDateTime;

public record FriendRequestDto(
        Long id,
        Long fromUserId,
        String fromUsername,
        String message,
        OffsetDateTime createdAt
) {
    public static FriendRequestDto from(FriendRequest r) {
        return new FriendRequestDto(
                r.getId(),
                r.getFromUser().getId(),
                r.getFromUser().getUsername(),
                r.getMessage(),
                r.getCreatedAt()
        );
    }
}
