package com.chatapp.message.repository;

import com.chatapp.message.entity.UnreadCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnreadCountRepository extends JpaRepository<UnreadCount, Long> {

    Optional<UnreadCount> findByUserIdAndRoomId(Long userId, Long roomId);

    Optional<UnreadCount> findByUserIdAndPartnerId(Long userId, Long partnerId);

    List<UnreadCount> findByUserId(Long userId);

    void deleteByUserIdAndRoomId(Long userId, Long roomId);

    void deleteByUserIdAndPartnerId(Long userId, Long partnerId);
}
