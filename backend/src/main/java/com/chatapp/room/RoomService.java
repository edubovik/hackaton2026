package com.chatapp.room;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.room.dto.*;
import com.chatapp.room.entity.*;
import com.chatapp.room.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBanRepository roomBanRepository;
    private final RoomInvitationRepository roomInvitationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       RoomBanRepository roomBanRepository,
                       RoomInvitationRepository roomInvitationRepository,
                       UserRepository userRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomBanRepository = roomBanRepository;
        this.roomInvitationRepository = roomInvitationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public RoomSummaryDto createRoom(User owner, CreateRoomRequest req) {
        if (roomRepository.existsByNameIgnoreCase(req.name())) {
            throw new BadRequestException("Room name already taken");
        }

        Room room = roomRepository.save(new Room(req.name(), req.description(), req.isPublic(), owner));
        roomMemberRepository.save(new RoomMember(room, owner, RoomMemberRole.OWNER));
        return RoomSummaryDto.from(room);
    }

    @Transactional(readOnly = true)
    public Page<RoomSummaryDto> listPublicRooms(String search, int page, int size) {
        return roomRepository.findPublicRooms(search, PageRequest.of(page, size))
                .map(RoomSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public RoomDetailDto getRoomDetail(User requester, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found"));

        boolean isMember = roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, requester.getId());

        if (!room.isPublic() && !isMember) {
            throw new ForbiddenException("Not a member of this private room");
        }

        List<MemberDto> members = isMember
                ? roomMemberRepository.findByRoom_Id(roomId).stream().map(MemberDto::from).toList()
                : List.of();

        return RoomDetailDto.from(room, members);
    }

    @Transactional
    public RoomSummaryDto updateRoom(User requester, Long roomId, UpdateRoomRequest req) {
        Room room = requireRoom(roomId);
        requireRole(roomId, requester.getId(), List.of(RoomMemberRole.OWNER));

        if (req.name() != null && !req.name().equalsIgnoreCase(room.getName())) {
            if (roomRepository.existsByNameIgnoreCase(req.name())) {
                throw new BadRequestException("Room name already taken");
            }
            room.setName(req.name());
        }
        if (req.description() != null) room.setDescription(req.description());
        if (req.isPublic() != null) room.setPublic(req.isPublic());

        return RoomSummaryDto.from(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(User requester, Long roomId) {
        Room room = requireRoom(roomId);
        requireRole(roomId, requester.getId(), List.of(RoomMemberRole.OWNER));
        roomRepository.delete(room);
    }

    @Transactional
    public void joinRoom(User user, Long roomId) {
        Room room = roomRepository.findByIdAndIsPublicTrue(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found or not public"));

        if (roomBanRepository.existsByRoom_IdAndUser_Id(roomId, user.getId())) {
            throw new ForbiddenException("You are banned from this room");
        }
        if (roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, user.getId())) {
            throw new BadRequestException("Already a member");
        }

        roomMemberRepository.save(new RoomMember(room, user, RoomMemberRole.MEMBER));
        publishMemberEvent(roomId, user, "JOIN");
    }

    @Transactional
    public void leaveRoom(User user, Long roomId) {
        Room room = requireRoom(roomId);

        if (room.getOwner().getId().equals(user.getId())) {
            throw new BadRequestException("Owner cannot leave — delete the room instead");
        }
        requireMember(roomId, user.getId());

        roomMemberRepository.deleteByRoom_IdAndUser_Id(roomId, user.getId());
        publishMemberEvent(roomId, user, "LEAVE");
    }

    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(User requester, Long roomId) {
        requireRoom(roomId);
        requireMember(roomId, requester.getId());
        return roomMemberRepository.findByRoom_Id(roomId).stream().map(MemberDto::from).toList();
    }

    @Transactional
    public void inviteUser(User inviter, Long roomId, String inviteeUsername) {
        requireRoom(roomId);
        requireMember(roomId, inviter.getId());

        User invitee = userRepository.findByUsername(inviteeUsername)
                .orElseThrow(() -> new BadRequestException("User not found: " + inviteeUsername));

        if (roomBanRepository.existsByRoom_IdAndUser_Id(roomId, invitee.getId())) {
            throw new ForbiddenException("User is banned from this room");
        }
        if (roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, invitee.getId())) {
            throw new BadRequestException("User is already a member");
        }
        if (roomInvitationRepository.existsByRoom_IdAndInvitee_Id(roomId, invitee.getId())) {
            throw new BadRequestException("Invitation already sent");
        }

        Room room = requireRoom(roomId);
        roomInvitationRepository.save(new RoomInvitation(room, inviter, invitee));
    }

    @Transactional
    public void acceptInvitation(User invitee, Long roomId) {
        RoomInvitation invitation = roomInvitationRepository
                .findByRoom_IdAndInvitee_Id(roomId, invitee.getId())
                .orElseThrow(() -> new BadRequestException("Invitation not found"));

        if (roomBanRepository.existsByRoom_IdAndUser_Id(roomId, invitee.getId())) {
            throw new ForbiddenException("You are banned from this room");
        }
        if (roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, invitee.getId())) {
            throw new BadRequestException("Already a member");
        }

        Room room = invitation.getRoom();
        roomMemberRepository.save(new RoomMember(room, invitee, RoomMemberRole.MEMBER));
        roomInvitationRepository.delete(invitation);
        publishMemberEvent(roomId, invitee, "JOIN");
    }

    // ---- helpers ----

    Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BadRequestException("Room not found"));
    }

    void requireMember(Long roomId, Long userId) {
        if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
            throw new ForbiddenException("Not a member of this room");
        }
    }

    void requireRole(Long roomId, Long userId, List<RoomMemberRole> roles) {
        if (!roomMemberRepository.existsByRoom_IdAndUser_IdAndRoleIn(roomId, userId, roles)) {
            throw new ForbiddenException("Insufficient permissions");
        }
    }

    private void publishMemberEvent(Long roomId, User user, String type) {
        messagingTemplate.convertAndSend(
                "/topic/room." + roomId + ".members",
                Map.of("type", type, "userId", user.getId(), "username", user.getUsername()));
    }
}
