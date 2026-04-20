package com.chatapp.contact.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "friendships")
@IdClass(Friendship.FriendshipId.class)
public class Friendship {

    @Id
    @Column(name = "user_id_a", nullable = false)
    private Long userIdA;

    @Id
    @Column(name = "user_id_b", nullable = false)
    private Long userIdB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Friendship() {}

    public Friendship(Long userIdA, Long userIdB) {
        this.userIdA = userIdA;
        this.userIdB = userIdB;
    }

    public Long getUserIdA() { return userIdA; }
    public Long getUserIdB() { return userIdB; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static class FriendshipId implements Serializable {
        private Long userIdA;
        private Long userIdB;

        public FriendshipId() {}
        public FriendshipId(Long userIdA, Long userIdB) {
            this.userIdA = userIdA;
            this.userIdB = userIdB;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FriendshipId that)) return false;
            return java.util.Objects.equals(userIdA, that.userIdA) &&
                   java.util.Objects.equals(userIdB, that.userIdB);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userIdA, userIdB);
        }
    }
}
