package com.chatapp.room.dto;

import com.chatapp.room.entity.Room;

import java.time.OffsetDateTime;
import java.util.List;

public record RoomDetailDto(Long id, String name, String description, boolean isPublic, Long ownerId,
                            OffsetDateTime createdAt, List<MemberDto> members) {

    public static RoomDetailDto from(Room room, List<MemberDto> members) {
        return new RoomDetailDto(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.isPublic(),
                room.getOwner().getId(),
                room.getCreatedAt(),
                members
        );
    }
}
