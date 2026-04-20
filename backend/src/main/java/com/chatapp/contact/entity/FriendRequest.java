package com.chatapp.contact.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "friend_requests")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Column
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "friend_request_status")
    private FriendRequestStatus status = FriendRequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public FriendRequest() {}

    public FriendRequest(User fromUser, User toUser, String message) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
    }

    public Long getId() { return id; }
    public User getFromUser() { return fromUser; }
    public User getToUser() { return toUser; }
    public String getMessage() { return message; }
    public FriendRequestStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setStatus(FriendRequestStatus status) { this.status = status; }
}
