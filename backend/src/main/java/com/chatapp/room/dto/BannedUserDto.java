package com.chatapp.room.dto;

import com.chatapp.room.entity.RoomBan;

import java.time.OffsetDateTime;

public record BannedUserDto(Long userId, String username, Long bannedById, OffsetDateTime createdAt) {

    public static BannedUserDto from(RoomBan ban) {
        return new BannedUserDto(
                ban.getUser().getId(),
                ban.getUser().getUsername(),
                ban.getBannedBy().getId(),
                ban.getCreatedAt()
        );
    }
}
