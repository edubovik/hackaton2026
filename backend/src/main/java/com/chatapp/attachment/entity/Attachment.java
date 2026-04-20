package com.chatapp.attachment.entity;

import com.chatapp.auth.entity.User;
import com.chatapp.message.entity.Message;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private String filename;

    @Column(name = "stored_name", nullable = false, unique = true)
    private String storedName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Attachment() {}

    public Long getId() { return id; }
    public Message getMessage() { return message; }
    public User getUploader() { return uploader; }
    public String getFilename() { return filename; }
    public String getStoredName() { return storedName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setMessage(Message message) { this.message = message; }
    public void setUploader(User uploader) { this.uploader = uploader; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setComment(String comment) { this.comment = comment; }
}
