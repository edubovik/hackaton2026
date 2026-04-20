package com.chatapp.message;

import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.contact.repository.UserBanRepository;
import com.chatapp.message.dto.ChatMessageEvent;
import com.chatapp.message.dto.SendMessageRequest;
import com.chatapp.room.repository.RoomMemberRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
public class MessageStompController {

    private final RoomMemberRepository roomMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBanRepository userBanRepository;
    private final MessagePublisher messagePublisher;

    public MessageStompController(RoomMemberRepository roomMemberRepository,
                                  FriendshipRepository friendshipRepository,
                                  UserBanRepository userBanRepository,
                                  MessagePublisher messagePublisher) {
        this.roomMemberRepository = roomMemberRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBanRepository = userBanRepository;
        this.messagePublisher = messagePublisher;
    }

    @MessageMapping("/chat.room.{roomId}")
    public void sendToRoom(@DestinationVariable Long roomId,
                           SendMessageRequest req,
                           StompHeaderAccessor accessor) {
        User sender = extractUser(accessor);
        validateContent(req.content());

        if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, sender.getId())) {
            throw new ForbiddenException("Not a member of this room");
        }

        messagePublisher.publishRoomMessage(new ChatMessageEvent(
                "ROOM", roomId, sender.getId(), sender.getUsername(),
                null, req.content(), req.replyToId()));
    }

    @MessageMapping("/chat.dm.{partnerId}")
    public void sendDm(@DestinationVariable Long partnerId,
                       SendMessageRequest req,
                       StompHeaderAccessor accessor) {
        User sender = extractUser(accessor);
        validateContent(req.content());

        if (!friendshipRepository.existsBetween(sender.getId(), partnerId)) {
            throw new ForbiddenException("Not friends with this user");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(partnerId, sender.getId())) {
            throw new ForbiddenException("You are blocked by this user");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(sender.getId(), partnerId)) {
            throw new ForbiddenException("You have blocked this user");
        }

        messagePublisher.publishDmMessage(new ChatMessageEvent(
                "DM", null, sender.getId(), sender.getUsername(),
                partnerId, req.content(), req.replyToId()));
    }

    private User extractUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ForbiddenException("Not authenticated");
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Message content cannot be blank");
        }
        if (content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 3072) {
            throw new BadRequestException("Message exceeds 3 KB limit");
        }
    }
}
