package com.chatapp.room.repository;

import com.chatapp.room.entity.RoomMember;
import com.chatapp.room.entity.RoomMemberId;
import com.chatapp.room.entity.RoomMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    Optional<RoomMember> findByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

    List<RoomMember> findByRoom_Id(Long roomId);

    void deleteByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoom_IdAndUser_IdAndRoleIn(Long roomId, Long userId, List<RoomMemberRole> roles);

    List<RoomMember> findByUser_Id(Long userId);

    long countByRoom_Id(Long roomId);

    @Query("SELECT m.room.id, COUNT(m) FROM RoomMember m WHERE m.room.id IN :roomIds GROUP BY m.room.id")
    List<Object[]> countByRoomIds(@Param("roomIds") List<Long> roomIds);
}
