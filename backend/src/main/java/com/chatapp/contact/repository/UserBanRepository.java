package com.chatapp.contact.repository;

import com.chatapp.contact.entity.UserBan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBanRepository extends JpaRepository<UserBan, UserBan.UserBanId> {

    boolean existsByBannerIdAndBannedId(Long bannerId, Long bannedId);

    void deleteByBannerIdAndBannedId(Long bannerId, Long bannedId);
}
