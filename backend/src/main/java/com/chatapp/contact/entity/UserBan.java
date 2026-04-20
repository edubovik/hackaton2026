package com.chatapp.contact.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_bans")
@IdClass(UserBan.UserBanId.class)
public class UserBan {

    @Id
    @Column(name = "banner_id", nullable = false)
    private Long bannerId;

    @Id
    @Column(name = "banned_id", nullable = false)
    private Long bannedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UserBan() {}

    public UserBan(Long bannerId, Long bannedId) {
        this.bannerId = bannerId;
        this.bannedId = bannedId;
    }

    public Long getBannerId() { return bannerId; }
    public Long getBannedId() { return bannedId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static class UserBanId implements Serializable {
        private Long bannerId;
        private Long bannedId;

        public UserBanId() {}
        public UserBanId(Long bannerId, Long bannedId) {
            this.bannerId = bannerId;
            this.bannedId = bannedId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UserBanId that)) return false;
            return java.util.Objects.equals(bannerId, that.bannerId) &&
                   java.util.Objects.equals(bannedId, that.bannedId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(bannerId, bannedId);
        }
    }
}
