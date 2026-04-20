package com.chatapp.room.dto;

import com.chatapp.room.entity.Room;

import java.time.OffsetDateTime;

public record RoomSummaryDto(Long id, String name, String description, boolean isPublic, Long ownerId, OffsetDateTime createdAt, int memberCount) {

    public static RoomSummaryDto from(Room room) {
        return new RoomSummaryDto(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.isPublic(),
                room.getOwner().getId(),
                room.getCreatedAt(),
                0
        );
    }

    public static RoomSummaryDto from(Room room, int memberCount) {
        return new RoomSummaryDto(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.isPublic(),
                room.getOwner().getId(),
                room.getCreatedAt(),
                memberCount
        );
    }
}
