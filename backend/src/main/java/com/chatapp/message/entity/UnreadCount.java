package com.chatapp.message.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "unread_counts")
public class UnreadCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(nullable = false)
    private int count = 0;

    public UnreadCount() {}

    public static UnreadCount forRoom(Long userId, Long roomId) {
        UnreadCount u = new UnreadCount();
        u.userId = userId;
        u.roomId = roomId;
        return u;
    }

    public static UnreadCount forDm(Long userId, Long partnerId) {
        UnreadCount u = new UnreadCount();
        u.userId = userId;
        u.partnerId = partnerId;
        return u;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getRoomId() { return roomId; }
    public Long getPartnerId() { return partnerId; }
    public int getCount() { return count; }

    public void increment() { this.count++; }
    public void reset() { this.count = 0; }
}
