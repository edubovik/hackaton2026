package com.chatapp.attachment;

import com.chatapp.attachment.entity.Attachment;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.message.entity.Message;
import com.chatapp.room.repository.RoomMemberRepository;
import org.springframework.stereotype.Component;

@Component
public class AttachmentAccessGuard {

    private final RoomMemberRepository roomMemberRepository;

    public AttachmentAccessGuard(RoomMemberRepository roomMemberRepository) {
        this.roomMemberRepository = roomMemberRepository;
    }

    public void checkDownloadAccess(Long requesterId, Attachment attachment) {
        Message message = attachment.getMessage();
        if (message.getRoomId() != null) {
            if (!roomMemberRepository.existsByRoom_IdAndUser_Id(message.getRoomId(), requesterId)) {
                throw new ForbiddenException("Access denied: not a room member");
            }
        } else {
            // DM: requester must be sender or recipient
            boolean isSender = message.getSender().getId().equals(requesterId);
            boolean isRecipient = requesterId.equals(message.getRecipientId());
            if (!isSender && !isRecipient) {
                throw new ForbiddenException("Access denied");
            }
        }
    }
}
