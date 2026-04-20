package com.chatapp.contact.repository;

import com.chatapp.contact.entity.FriendRequest;
import com.chatapp.contact.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findByToUser_IdAndStatus(Long toUserId, FriendRequestStatus status);

    Optional<FriendRequest> findByFromUser_IdAndToUser_Id(Long fromUserId, Long toUserId);

    @Query("SELECT r FROM FriendRequest r WHERE " +
           "((r.fromUser.id = :a AND r.toUser.id = :b) OR (r.fromUser.id = :b AND r.toUser.id = :a)) " +
           "AND r.status = 'PENDING'")
    Optional<FriendRequest> findPendingBetween(@Param("a") Long a, @Param("b") Long b);
}
