package com.chatapp.message;

import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.message.entity.Message;
import com.chatapp.message.repository.MessageRepository;
import com.chatapp.message.repository.UnreadCountRepository;
import com.chatapp.room.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock UnreadCountRepository unreadCountRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    MessageService service;
    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        service = new MessageService(messageRepository, unreadCountRepository,
                roomMemberRepository, friendshipRepository, messagingTemplate);
        alice = makeUser(1L, "alice");
        bob   = makeUser(2L, "bob");
    }

    // ---- getRoomHistory: membership gate ----

    @Test
    void getRoomHistory_notMember_throwsForbidden() {
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(10L, alice.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getRoomHistory(alice, 10L, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getRoomHistory_member_delegatesToRepository() {
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(10L, alice.getId())).thenReturn(true);
        when(messageRepository.findRoomHistory(eq(10L), isNull(), any())).thenReturn(List.of());

        service.getRoomHistory(alice, 10L, null);

        verify(messageRepository).findRoomHistory(eq(10L), isNull(), any());
    }

    // ---- getDmHistory: friendship gate ----

    @Test
    void getDmHistory_notFriends_throwsForbidden() {
        when(friendshipRepository.existsBetween(alice.getId(), bob.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getDmHistory(alice, bob.getId(), null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getDmHistory_friends_delegatesToRepository() {
        when(friendshipRepository.existsBetween(alice.getId(), bob.getId())).thenReturn(true);
        when(messageRepository.findDmHistory(eq(alice.getId()), eq(bob.getId()), isNull(), any()))
                .thenReturn(List.of());

        service.getDmHistory(alice, bob.getId(), null);

        verify(messageRepository).findDmHistory(eq(alice.getId()), eq(bob.getId()), isNull(), any());
    }

    // ---- editMessage: author-only ----

    @Test
    void editMessage_nonAuthor_throwsForbidden() {
        Message msg = makeRoomMessage(100L, bob, 10L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> service.editMessage(alice, 100L, "new content"))
                .isInstanceOf(ForbiddenException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void editMessage_deletedMessage_throwsBadRequest() {
        Message msg = makeRoomMessage(100L, alice, 10L);
        msg.setDeleted(true);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> service.editMessage(alice, 100L, "new content"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void editMessage_author_savesAndBroadcasts() {
        Message msg = makeRoomMessage(100L, alice, 10L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(msg)).thenReturn(msg);

        service.editMessage(alice, 100L, "updated text");

        verify(messageRepository).save(msg);
        verify(messagingTemplate).convertAndSend(eq("/topic/room.10"), (Object) any());
    }

    // ---- deleteMessage: author + room admin ----

    @Test
    void deleteMessage_nonAuthorNonAdmin_throwsForbidden() {
        Message msg = makeRoomMessage(100L, bob, 10L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(alice.getId()), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.deleteMessage(alice, 100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteMessage_roomAdmin_succeeds() {
        Message msg = makeRoomMessage(100L, bob, 10L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(alice.getId()), any()))
                .thenReturn(true);
        when(messageRepository.save(msg)).thenReturn(msg);

        service.deleteMessage(alice, 100L);

        verify(messageRepository).save(msg);
    }

    @Test
    void deleteMessage_author_succeeds() {
        Message msg = makeRoomMessage(100L, alice, 10L);
        when(messageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(msg)).thenReturn(msg);

        service.deleteMessage(alice, 100L);

        verify(messageRepository).save(msg);
    }

    @Test
    void deleteMessage_dmNonAuthor_throwsForbidden() {
        Message msg = makeDmMessage(200L, bob, alice.getId());
        when(messageRepository.findById(200L)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> service.deleteMessage(alice, 200L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- helpers ----

    private static User makeUser(Long id, String username) {
        try {
            User u = new User();
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
            u.setUsername(username);
            u.setEmail(username + "@test.com");
            u.setPassword("pw");
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Message makeRoomMessage(Long id, User sender, Long roomId) {
        try {
            Message m = new Message();
            var f = Message.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
            m.setSender(sender);
            m.setRoomId(roomId);
            m.setContent("hello");
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Message makeDmMessage(Long id, User sender, Long recipientId) {
        try {
            Message m = new Message();
            var f = Message.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(m, id);
            m.setSender(sender);
            m.setRecipientId(recipientId);
            m.setContent("hello dm");
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
