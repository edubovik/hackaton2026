package com.chatapp.room.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "room_members")
@IdClass(RoomMemberId.class)
public class RoomMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "room_member_role")
    private RoomMemberRole role = RoomMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    public RoomMember() {}

    public RoomMember(Room room, User user, RoomMemberRole role) {
        this.room = room;
        this.user = user;
        this.role = role;
    }

    public Room getRoom() { return room; }
    public User getUser() { return user; }
    public RoomMemberRole getRole() { return role; }
    public OffsetDateTime getJoinedAt() { return joinedAt; }

    public void setRole(RoomMemberRole role) { this.role = role; }
}
