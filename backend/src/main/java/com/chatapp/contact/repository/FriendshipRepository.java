package com.chatapp.contact.repository;

import com.chatapp.contact.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Friendship.FriendshipId> {

    @Query("SELECT f FROM Friendship f WHERE f.userIdA = :userId OR f.userIdB = :userId")
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
           "WHERE (f.userIdA = :a AND f.userIdB = :b) OR (f.userIdA = :b AND f.userIdB = :a)")
    boolean existsBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("DELETE FROM Friendship f WHERE " +
           "(f.userIdA = :a AND f.userIdB = :b) OR (f.userIdA = :b AND f.userIdB = :a)")
    @org.springframework.data.jpa.repository.Modifying
    void deleteBetween(@Param("a") Long a, @Param("b") Long b);
}
