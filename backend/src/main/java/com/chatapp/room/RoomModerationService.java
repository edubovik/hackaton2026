package com.chatapp.room;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.room.dto.BannedUserDto;
import com.chatapp.room.entity.RoomBan;
import com.chatapp.room.entity.RoomMember;
import com.chatapp.room.entity.RoomMemberRole;
import com.chatapp.room.repository.RoomBanRepository;
import com.chatapp.room.repository.RoomMemberRepository;
import com.chatapp.common.BrokerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RoomModerationService {

    private final RoomService roomService;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBanRepository roomBanRepository;
    private final UserRepository userRepository;
    private final BrokerTemplate messagingTemplate;

    public RoomModerationService(RoomService roomService,
                                 RoomMemberRepository roomMemberRepository,
                                 RoomBanRepository roomBanRepository,
                                 UserRepository userRepository,
                                 BrokerTemplate messagingTemplate) {
        this.roomService = roomService;
        this.roomMemberRepository = roomMemberRepository;
        this.roomBanRepository = roomBanRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void banMember(User admin, Long roomId, Long targetUserId) {
        requireAdminOrOwner(roomId, admin.getId());

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        RoomMember targetMember = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, targetUserId)
                .orElseThrow(() -> new BadRequestException("User is not a member"));

        if (targetMember.getRole() == RoomMemberRole.OWNER) {
            throw new ForbiddenException("Cannot ban the room owner");
        }

        // Admin cannot ban another admin unless they are the owner
        if (targetMember.getRole() == RoomMemberRole.ADMIN) {
            roomService.requireRole(roomId, admin.getId(), List.of(RoomMemberRole.OWNER));
        }

        roomMemberRepository.deleteByRoom_IdAndUser_Id(roomId, targetUserId);
        var room = roomService.requireRoom(roomId);
        roomBanRepository.save(new RoomBan(room, target, admin));

        messagingTemplate.send(
                "/topic/room." + roomId + ".members",
                Map.of("type", "BAN", "userId", target.getId(), "username", target.getUsername()));
    }

    @Transactional
    public void unbanMember(User admin, Long roomId, Long targetUserId) {
        requireAdminOrOwner(roomId, admin.getId());
        roomBanRepository.deleteByRoom_IdAndUser_Id(roomId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<BannedUserDto> listBans(User requester, Long roomId) {
        requireAdminOrOwner(roomId, requester.getId());
        return roomBanRepository.findByRoom_Id(roomId).stream().map(BannedUserDto::from).toList();
    }

    @Transactional
    public void promoteToAdmin(User owner, Long roomId, Long targetUserId) {
        roomService.requireRole(roomId, owner.getId(), List.of(RoomMemberRole.OWNER));

        RoomMember member = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, targetUserId)
                .orElseThrow(() -> new BadRequestException("User is not a member"));

        if (member.getRole() == RoomMemberRole.OWNER) {
            throw new BadRequestException("User is already the owner");
        }

        member.setRole(RoomMemberRole.ADMIN);
        roomMemberRepository.save(member);
    }

    @Transactional
    public void demoteAdmin(User owner, Long roomId, Long targetUserId) {
        roomService.requireRole(roomId, owner.getId(), List.of(RoomMemberRole.OWNER));

        RoomMember member = roomMemberRepository.findByRoom_IdAndUser_Id(roomId, targetUserId)
                .orElseThrow(() -> new BadRequestException("User is not a member"));

        if (member.getRole() == RoomMemberRole.OWNER) {
            throw new ForbiddenException("Cannot demote the owner");
        }

        member.setRole(RoomMemberRole.MEMBER);
        roomMemberRepository.save(member);
    }

    private void requireAdminOrOwner(Long roomId, Long userId) {
        roomService.requireRole(roomId, userId, List.of(RoomMemberRole.ADMIN, RoomMemberRole.OWNER));
    }
}
