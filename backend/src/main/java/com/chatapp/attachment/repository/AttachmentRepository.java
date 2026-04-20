package com.chatapp.attachment.repository;

import com.chatapp.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMessage_IdIn(List<Long> messageIds);
}
