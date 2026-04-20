package com.chatapp.room;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.room.dto.CreateRoomRequest;
import com.chatapp.room.entity.*;
import com.chatapp.room.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.chatapp.common.BrokerTemplate;

import com.chatapp.room.dto.RoomSummaryDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock RoomBanRepository roomBanRepository;
    @Mock RoomInvitationRepository roomInvitationRepository;
    @Mock UserRepository userRepository;
    @Mock BrokerTemplate messagingTemplate;

    RoomService service;
    User owner;
    User alice;

    @BeforeEach
    void setUp() {
        service = new RoomService(roomRepository, roomMemberRepository, roomBanRepository,
                roomInvitationRepository, userRepository, messagingTemplate);
        owner = makeUser(1L, "owner");
        alice = makeUser(2L, "alice");
    }

    @Test
    void createRoom_duplicateName_throwsBadRequest() {
        when(roomRepository.existsByNameIgnoreCase("General")).thenReturn(true);

        assertThatThrownBy(() -> service.createRoom(owner, new CreateRoomRequest("General", null, true)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already taken");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void joinRoom_bannedUser_throwsForbidden() {
        Room room = makeRoom(10L, owner, true);
        when(roomRepository.findByIdAndIsPublicTrue(10L)).thenReturn(Optional.of(room));
        when(roomBanRepository.existsByRoom_IdAndUser_Id(10L, alice.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.joinRoom(alice, 10L))
                .isInstanceOf(ForbiddenException.class);

        verify(roomMemberRepository, never()).save(any());
    }

    @Test
    void joinRoom_alreadyMember_throwsBadRequest() {
        Room room = makeRoom(10L, owner, true);
        when(roomRepository.findByIdAndIsPublicTrue(10L)).thenReturn(Optional.of(room));
        when(roomBanRepository.existsByRoom_IdAndUser_Id(10L, alice.getId())).thenReturn(false);
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(10L, alice.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.joinRoom(alice, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Already a member");
    }

    @Test
    void leaveRoom_ownerCannotLeave_throwsBadRequest() {
        Room room = makeRoom(10L, owner, true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.leaveRoom(owner, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Owner cannot leave");
    }

    @Test
    void deleteRoom_nonOwner_throwsForbidden() {
        Room room = makeRoom(10L, owner, true);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(eq(10L), eq(alice.getId()), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.deleteRoom(alice, 10L))
                .isInstanceOf(ForbiddenException.class);

        verify(roomRepository, never()).delete(any());
    }

    @Test
    void getMyRooms_returnsRoomsForUser() {
        Room room1 = makeRoom(10L, owner, true);
        Room room2 = makeRoom(11L, alice, false);
        RoomMember m1 = new RoomMember(room1, owner, RoomMemberRole.OWNER);
        RoomMember m2 = new RoomMember(room2, owner, RoomMemberRole.MEMBER);
        when(roomMemberRepository.findByUser_Id(owner.getId())).thenReturn(List.of(m1, m2));

        List<RoomSummaryDto> result = service.getMyRooms(owner);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoomSummaryDto::id).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void joinRoom_bannedFromPrivateRoom_throwsForbidden() {
        when(roomRepository.findByIdAndIsPublicTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinRoom(alice, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not public");
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

    private static Room makeRoom(Long id, User owner, boolean isPublic) {
        try {
            Room r = new Room("room" + id, null, isPublic, owner);
            var f = Room.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
