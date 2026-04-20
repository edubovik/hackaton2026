package com.chatapp.message.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "recipient_id")
    private Long recipientId;

    @Column(name = "reply_to_id")
    private Long replyToId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Message() {}

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public User getSender() { return sender; }
    public Long getRecipientId() { return recipientId; }
    public Long getReplyToId() { return replyToId; }
    public String getContent() { return content; }
    public boolean isEdited() { return edited; }
    public boolean isDeleted() { return deleted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setSender(User sender) { this.sender = sender; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }
    public void setContent(String content) { this.content = content; }
    public void setEdited(boolean edited) { this.edited = edited; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
