package com.chatapp.room.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "room_bans")
@IdClass(RoomBanId.class)
public class RoomBan {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banned_by", nullable = false)
    private User bannedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RoomBan() {}

    public RoomBan(Room room, User user, User bannedBy) {
        this.room = room;
        this.user = user;
        this.bannedBy = bannedBy;
    }

    public Room getRoom() { return room; }
    public User getUser() { return user; }
    public User getBannedBy() { return bannedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
