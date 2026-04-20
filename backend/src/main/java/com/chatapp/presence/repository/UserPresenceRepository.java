package com.chatapp.presence.repository;

import com.chatapp.presence.entity.PresenceState;
import com.chatapp.presence.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {

    @Query("SELECT p FROM UserPresence p WHERE p.state <> :offline AND p.updatedAt < :cutoff")
    List<UserPresence> findStaleActive(PresenceState offline, Instant cutoff);
}
