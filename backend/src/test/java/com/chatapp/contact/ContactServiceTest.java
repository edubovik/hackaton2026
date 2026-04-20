package com.chatapp.contact;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.entity.FriendRequest;
import com.chatapp.contact.entity.UserBan;
import com.chatapp.contact.repository.FriendRequestRepository;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.contact.repository.UserBanRepository;
import com.chatapp.presence.repository.UserPresenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock FriendRequestRepository friendRequestRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock UserBanRepository userBanRepository;
    @Mock UserRepository userRepository;
    @Mock UserPresenceRepository presenceRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    ContactService service;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        service = new ContactService(
                friendRequestRepository, friendshipRepository, userBanRepository,
                userRepository, presenceRepository, messagingTemplate);

        alice = makeUser(1L, "alice");
        bob = makeUser(2L, "bob");
    }

    @Test
    void sendRequest_blockedByBan_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userBanRepository.existsByBannerIdAndBannedId(alice.getId(), bob.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.sendRequest(bob, "alice", null))
                .isInstanceOf(ForbiddenException.class);

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void sendRequest_duplicate_throwsBadRequest() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userBanRepository.existsByBannerIdAndBannedId(bob.getId(), alice.getId())).thenReturn(false);
        when(friendRequestRepository.findByFromUser_IdAndToUser_Id(alice.getId(), bob.getId()))
                .thenReturn(Optional.of(new FriendRequest(alice, bob, null)));

        assertThatThrownBy(() -> service.sendRequest(alice, "bob", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already sent");

        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void banUser_removesFriendship() {
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(friendshipRepository.existsBetween(alice.getId(), bob.getId())).thenReturn(true);

        service.banUser(alice, bob.getId());

        verify(friendshipRepository).deleteBetween(alice.getId(), bob.getId());
        verify(userBanRepository).save(any(UserBan.class));
    }

    @Test
    void banUser_noFriendship_stillSavesBan() {
        when(userRepository.findById(bob.getId())).thenReturn(Optional.of(bob));
        when(friendshipRepository.existsBetween(alice.getId(), bob.getId())).thenReturn(false);

        service.banUser(alice, bob.getId());

        verify(friendshipRepository, never()).deleteBetween(any(), any());
        verify(userBanRepository).save(any(UserBan.class));
    }

    @Test
    void acceptRequest_wrongUser_throwsForbidden() {
        FriendRequest request = new FriendRequest(alice, bob, null);
        setId(request, 10L);
        when(friendRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.acceptRequest(alice, 10L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- helpers ----

    private static User makeUser(Long id, String username) {
        try {
            User u = new User();
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
            u.setUsername(username);
            u.setEmail(username + "@test.com");
            u.setPassword("pw");
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setId(FriendRequest r, Long id) {
        try {
            var f = FriendRequest.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
