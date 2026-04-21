package com.chatapp.message.repository;

import com.chatapp.message.entity.UnreadCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnreadCountRepository extends JpaRepository<UnreadCount, Long> {

    /**
     * Single bulk UPSERT: increments the unread counter for every room member
     * except the sender. Replaces the previous N-query loop and brings DB cost
     * from O(members) round-trips down to O(1) per message.
     */
    @Modifying
    @Query(value = """
            INSERT INTO unread_counts (user_id, room_id, count)
            SELECT rm.user_id, :roomId, 1
              FROM room_members rm
             WHERE rm.room_id = :roomId
               AND rm.user_id <> :senderId
            ON CONFLICT (user_id, room_id) WHERE room_id IS NOT NULL
            DO UPDATE SET count = unread_counts.count + 1
            """, nativeQuery = true)
    void bulkIncrementRoomUnread(@Param("roomId") Long roomId, @Param("senderId") Long senderId);

    Optional<UnreadCount> findByUserIdAndRoomId(Long userId, Long roomId);

    Optional<UnreadCount> findByUserIdAndPartnerId(Long userId, Long partnerId);

    List<UnreadCount> findByUserId(Long userId);

    void deleteByUserIdAndRoomId(Long userId, Long roomId);

    void deleteByUserIdAndPartnerId(Long userId, Long partnerId);
}
