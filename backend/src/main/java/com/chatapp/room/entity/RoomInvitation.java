package com.chatapp.room.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "room_invitations")
public class RoomInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RoomInvitation() {}

    public RoomInvitation(Room room, User inviter, User invitee) {
        this.room = room;
        this.inviter = inviter;
        this.invitee = invitee;
    }

    public Long getId() { return id; }
    public Room getRoom() { return room; }
    public User getInviter() { return inviter; }
    public User getInvitee() { return invitee; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
