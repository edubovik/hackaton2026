package com.chatapp.presence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_presence")
public class UserPresence {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PresenceState state = PresenceState.OFFLINE;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserPresence() {}

    public UserPresence(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public PresenceState getState() { return state; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setState(PresenceState state) {
        this.state = state;
        this.updatedAt = Instant.now();
    }
}
