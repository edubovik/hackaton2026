package com.chatapp.presence.repository;

import com.chatapp.presence.entity.PresenceState;
import com.chatapp.presence.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {

    List<UserPresence> findByStateAndUpdatedAtBefore(PresenceState state, Instant cutoff);
}
