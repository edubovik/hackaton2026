package com.chatapp.room;

import com.chatapp.auth.entity.User;
import com.chatapp.room.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomModerationService roomModerationService;

    public RoomController(RoomService roomService, RoomModerationService roomModerationService) {
        this.roomService = roomService;
        this.roomModerationService = roomModerationService;
    }

    @PostMapping
    public ResponseEntity<RoomSummaryDto> createRoom(
            @AuthenticationPrincipal User user,
            @RequestBody CreateRoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(user, req));
    }

    @GetMapping
    public Page<RoomSummaryDto> listRooms(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return roomService.listPublicRooms(search, page, size);
    }

    @GetMapping("/{id}")
    public RoomDetailDto getRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return roomService.getRoomDetail(user, id);
    }

    @PutMapping("/{id}")
    public RoomSummaryDto updateRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody UpdateRoomRequest req) {
        return roomService.updateRoom(user, id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        roomService.deleteRoom(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        roomService.joinRoom(user, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        roomService.leaveRoom(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    public List<MemberDto> listMembers(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return roomService.listMembers(user, id);
    }

    @PostMapping("/{id}/members/{userId}/ban")
    public ResponseEntity<Void> banMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long userId) {
        roomModerationService.banMember(user, id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}/ban")
    public ResponseEntity<Void> unbanMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long userId) {
        roomModerationService.unbanMember(user, id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bans")
    public List<BannedUserDto> listBans(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return roomModerationService.listBans(user, id);
    }

    @PostMapping("/{id}/admins/{userId}")
    public ResponseEntity<Void> promoteAdmin(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long userId) {
        roomModerationService.promoteToAdmin(user, id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/admins/{userId}")
    public ResponseEntity<Void> demoteAdmin(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long userId) {
        roomModerationService.demoteAdmin(user, id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<Void> inviteUser(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody InviteRequest req) {
        roomService.inviteUser(user, id, req.username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/invitations/accept")
    public ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        roomService.acceptInvitation(user, id);
        return ResponseEntity.ok().build();
    }
}
