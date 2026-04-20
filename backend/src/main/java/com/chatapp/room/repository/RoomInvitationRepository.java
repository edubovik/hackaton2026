package com.chatapp.room.repository;

import com.chatapp.room.entity.RoomInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {

    boolean existsByRoom_IdAndInvitee_Id(Long roomId, Long inviteeId);

    Optional<RoomInvitation> findByRoom_IdAndInvitee_Id(Long roomId, Long inviteeId);

    List<RoomInvitation> findByRoom_Id(Long roomId);

    void deleteByRoom_IdAndInvitee_Id(Long roomId, Long inviteeId);
}
