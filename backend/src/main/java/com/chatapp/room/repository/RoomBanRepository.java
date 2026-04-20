package com.chatapp.room.repository;

import com.chatapp.room.entity.RoomBan;
import com.chatapp.room.entity.RoomBanId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomBanRepository extends JpaRepository<RoomBan, RoomBanId> {

    boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

    List<RoomBan> findByRoom_Id(Long roomId);

    void deleteByRoom_IdAndUser_Id(Long roomId, Long userId);
}
