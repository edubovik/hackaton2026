package com.chatapp.room.repository;

import com.chatapp.room.entity.RoomMember;
import com.chatapp.room.entity.RoomMemberId;
import com.chatapp.room.entity.RoomMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    Optional<RoomMember> findByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

    List<RoomMember> findByRoom_Id(Long roomId);

    void deleteByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoom_IdAndUser_IdAndRoleIn(Long roomId, Long userId, List<RoomMemberRole> roles);
}
