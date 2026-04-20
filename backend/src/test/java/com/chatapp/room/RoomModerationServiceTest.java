package com.chatapp.room;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.room.entity.*;
import com.chatapp.room.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.chatapp.common.BrokerTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomModerationServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock RoomBanRepository roomBanRepository;
    @Mock RoomInvitationRepository roomInvitationRepository;
    @Mock UserRepository userRepository;
    @Mock BrokerTemplate messagingTemplate;

    RoomService roomService;
    RoomModerationService moderationService;

    User owner;
    User admin;
    User member;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, roomMemberRepository, roomBanRepository,
                roomInvitationRepository, userRepository, messagingTemplate);
        moderationService = new RoomModerationService(roomService, roomMemberRepository,
                roomBanRepository, userRepository, messagingTemplate);

        owner = makeUser(1L, "owner");
        admin = makeUser(2L, "admin");
        member = makeUser(3L, "member");
    }

    @Test
    void banMember_byAdmin_removesAndBans() {
        Room room = makeRoom(10L, owner);
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(admin.getId()), any()))
                .thenReturn(true);
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        RoomMember memberRecord = new RoomMember(room, member, RoomMemberRole.MEMBER);
        when(roomMemberRepository.findByRoom_IdAndUser_Id(10L, member.getId()))
                .thenReturn(Optional.of(memberRecord));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        moderationService.banMember(admin, 10L, member.getId());

        verify(roomMemberRepository).deleteByRoom_IdAndUser_Id(10L, member.getId());
        verify(roomBanRepository).save(any(RoomBan.class));
    }

    @Test
    void banMember_ownerCannotBeBanned_throwsForbidden() {
        Room room = makeRoom(10L, owner);
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(admin.getId()), any()))
                .thenReturn(true);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        RoomMember ownerRecord = new RoomMember(room, owner, RoomMemberRole.OWNER);
        when(roomMemberRepository.findByRoom_IdAndUser_Id(10L, owner.getId()))
                .thenReturn(Optional.of(ownerRecord));

        assertThatThrownBy(() -> moderationService.banMember(admin, 10L, owner.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot ban the room owner");
    }

    @Test
    void promoteToAdmin_byNonOwner_throwsForbidden() {
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(admin.getId()), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> moderationService.promoteToAdmin(admin, 10L, member.getId()))
                .isInstanceOf(ForbiddenException.class);

        verify(roomMemberRepository, never()).save(any());
    }

    @Test
    void demoteAdmin_ownerCannot_throwsForbidden() {
        Room room = makeRoom(10L, owner);
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(owner.getId()), any()))
                .thenReturn(true);
        RoomMember ownerRecord = new RoomMember(room, owner, RoomMemberRole.OWNER);
        when(roomMemberRepository.findByRoom_IdAndUser_Id(10L, owner.getId()))
                .thenReturn(Optional.of(ownerRecord));

        assertThatThrownBy(() -> moderationService.demoteAdmin(owner, 10L, owner.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cannot demote the owner");
    }

    @Test
    void promoteToAdmin_alreadyOwner_throwsBadRequest() {
        Room room = makeRoom(10L, owner);
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(owner.getId()), any()))
                .thenReturn(true);
        RoomMember ownerRecord = new RoomMember(room, owner, RoomMemberRole.OWNER);
        when(roomMemberRepository.findByRoom_IdAndUser_Id(10L, owner.getId()))
                .thenReturn(Optional.of(ownerRecord));

        assertThatThrownBy(() -> moderationService.promoteToAdmin(owner, 10L, owner.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already the owner");
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

    private static Room makeRoom(Long id, User owner) {
        try {
            Room r = new Room("room" + id, null, true, owner);
            var f = Room.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
