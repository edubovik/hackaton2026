package com.chatapp.room.entity;

import com.chatapp.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Room() {}

    public Room(String name, String description, boolean isPublic, User owner) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isPublic() { return isPublic; }
    public User getOwner() { return owner; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
}
