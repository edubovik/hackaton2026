package com.chatapp.room.dto;

import com.chatapp.room.entity.RoomMember;

import java.time.OffsetDateTime;

public record MemberDto(Long userId, String username, String role, OffsetDateTime joinedAt) {

    public static MemberDto from(RoomMember member) {
        return new MemberDto(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole().name(),
                member.getJoinedAt()
        );
    }
}
