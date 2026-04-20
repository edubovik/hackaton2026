package com.chatapp.room.entity;

import java.io.Serializable;
import java.util.Objects;

public class RoomBanId implements Serializable {
    private Long room;
    private Long user;

    public RoomBanId() {}

    public RoomBanId(Long room, Long user) {
        this.room = room;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomBanId that)) return false;
        return Objects.equals(room, that.room) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() { return Objects.hash(room, user); }
}
