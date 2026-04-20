package com.chatapp.room.repository;

import com.chatapp.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT r FROM Room r WHERE r.isPublic = true AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Room> findPublicRooms(@Param("search") String search, Pageable pageable);

    Optional<Room> findByIdAndIsPublicTrue(Long id);
}
