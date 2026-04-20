package com.chatapp.message;

import com.chatapp.attachment.dto.AttachmentDto;
import com.chatapp.attachment.repository.AttachmentRepository;
import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.message.dto.MessageDto;
import com.chatapp.message.dto.MessagePage;
import com.chatapp.message.dto.UnreadCountDto;
import com.chatapp.message.entity.Message;
import com.chatapp.message.entity.UnreadCount;
import com.chatapp.message.repository.MessageRepository;
import com.chatapp.message.repository.UnreadCountRepository;
import com.chatapp.room.entity.RoomMemberRole;
import com.chatapp.room.repository.RoomMemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final int PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final UnreadCountRepository unreadCountRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final AttachmentRepository attachmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          UnreadCountRepository unreadCountRepository,
                          RoomMemberRepository roomMemberRepository,
                          FriendshipRepository friendshipRepository,
                          AttachmentRepository attachmentRepository,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.unreadCountRepository = unreadCountRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.friendshipRepository = friendshipRepository;
        this.attachmentRepository = attachmentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public MessagePage getRoomHistory(User requester, Long roomId, Long cursor) {
        requireRoomMember(roomId, requester.getId());
        List<Message> rows = messageRepository.findRoomHistory(roomId, cursor, PageRequest.of(0, PAGE_SIZE + 1));
        return toPage(rows);
    }

    @Transactional(readOnly = true)
    public MessagePage getDmHistory(User requester, Long partnerId, Long cursor) {
        requireFriendship(requester.getId(), partnerId);
        List<Message> rows = messageRepository.findDmHistory(requester.getId(), partnerId, cursor, PageRequest.of(0, PAGE_SIZE + 1));
        return toPage(rows);
    }

    @Transactional
    public MessageDto editMessage(User requester, Long messageId, String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new BadRequestException("Content cannot be blank");
        }
        Message message = requireMessage(messageId);
        if (!message.getSender().getId().equals(requester.getId())) {
            throw new ForbiddenException("Only the author can edit this message");
        }
        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }
        message.setContent(newContent);
        message.setEdited(true);
        message.setUpdatedAt(OffsetDateTime.now());
        Message saved = messageRepository.save(message);
        MessageDto dto = MessageDto.from(saved);
        broadcastEdit(dto);
        return dto;
    }

    @Transactional
    public void deleteMessage(User requester, Long messageId) {
        Message message = requireMessage(messageId);
        Long senderId = message.getSender().getId();
        boolean isAuthor = senderId.equals(requester.getId());
        boolean isRoomAdmin = message.getRoomId() != null &&
                roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(
                        message.getRoomId(), requester.getId(),
                        List.of(RoomMemberRole.OWNER, RoomMemberRole.ADMIN));

        if (!isAuthor && !isRoomAdmin) {
            throw new ForbiddenException("Not allowed to delete this message");
        }
        if (message.isDeleted()) return;

        message.setDeleted(true);
        message.setUpdatedAt(OffsetDateTime.now());
        Message saved = messageRepository.save(message);
        MessageDto dto = MessageDto.from(saved);
        broadcastEdit(dto);
    }

    @Transactional
    public void markRoomRead(User requester, Long roomId) {
        requireRoomMember(roomId, requester.getId());
        unreadCountRepository.deleteByUserIdAndRoomId(requester.getId(), roomId);
    }

    @Transactional
    public void markDmRead(User requester, Long partnerId) {
        unreadCountRepository.deleteByUserIdAndPartnerId(requester.getId(), partnerId);
    }

    @Transactional(readOnly = true)
    public List<UnreadCountDto> getUnreadCounts(User requester) {
        return unreadCountRepository.findByUserId(requester.getId()).stream()
                .map(u -> new UnreadCountDto(u.getRoomId(), u.getPartnerId(), u.getCount()))
                .toList();
    }

    @Transactional
    public void incrementRoomUnread(Long roomId, Long senderId) {
        roomMemberRepository.findByRoom_Id(roomId).forEach(member -> {
            Long userId = member.getUser().getId();
            if (userId.equals(senderId)) return;
            UnreadCount uc = unreadCountRepository.findByUserIdAndRoomId(userId, roomId)
                    .orElseGet(() -> unreadCountRepository.save(UnreadCount.forRoom(userId, roomId)));
            uc.increment();
            unreadCountRepository.save(uc);
        });
    }

    @Transactional
    public void incrementDmUnread(Long recipientId, Long partnerId) {
        UnreadCount uc = unreadCountRepository.findByUserIdAndPartnerId(recipientId, partnerId)
                .orElseGet(() -> unreadCountRepository.save(UnreadCount.forDm(recipientId, partnerId)));
        uc.increment();
        unreadCountRepository.save(uc);
    }

    // ---- helpers ----

    private MessagePage toPage(List<Message> rows) {
        boolean hasMore = rows.size() > PAGE_SIZE;
        List<Message> page = rows.stream().limit(PAGE_SIZE).toList();
        List<Long> ids = page.stream().map(Message::getId).toList();
        Map<Long, List<AttachmentDto>> byMessage = attachmentRepository.findByMessage_IdIn(ids).stream()
                .map(AttachmentDto::from)
                .collect(Collectors.groupingBy(AttachmentDto::messageId));
        List<MessageDto> dtos = page.stream()
                .map(m -> MessageDto.from(m, byMessage.getOrDefault(m.getId(), List.of())))
                .toList();
        return new MessagePage(dtos, hasMore);
    }

    private Message requireMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new BadRequestException("Message not found"));
    }

    private void requireRoomMember(Long roomId, Long userId) {
        if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
            throw new ForbiddenException("Not a member of this room");
        }
    }

    private void requireFriendship(Long a, Long b) {
        if (!friendshipRepository.existsBetween(a, b)) {
            throw new ForbiddenException("Not friends with this user");
        }
    }

    private void broadcastEdit(MessageDto dto) {
        if (dto.roomId() != null) {
            messagingTemplate.convertAndSend("/topic/room." + dto.roomId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.senderId()), "/queue/user." + dto.senderId(), dto);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.recipientId()), "/queue/user." + dto.recipientId(), dto);
        }
    }
}
