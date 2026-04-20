package com.chatapp.message.repository;

import com.chatapp.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.roomId = :roomId AND (:cursor IS NULL OR m.id < :cursor) ORDER BY m.id DESC")
    List<Message> findRoomHistory(@Param("roomId") Long roomId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE ((m.sender.id = :a AND m.recipientId = :b) OR (m.sender.id = :b AND m.recipientId = :a)) AND (:cursor IS NULL OR m.id < :cursor) ORDER BY m.id DESC")
    List<Message> findDmHistory(@Param("a") Long a, @Param("b") Long b, @Param("cursor") Long cursor, Pageable pageable);
}
