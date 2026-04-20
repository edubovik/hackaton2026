package com.chatapp.auth.repository;

import com.chatapp.auth.entity.RefreshToken;
import com.chatapp.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUser(User user);
    void deleteByToken(String token);
    void deleteByUser(User user);
}
