package com.chatapp.contact;

import com.chatapp.auth.entity.User;
import com.chatapp.contact.dto.FriendDto;
import com.chatapp.contact.dto.FriendRequestDto;
import com.chatapp.contact.dto.SendFriendRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/requests")
    public ResponseEntity<FriendRequestDto> sendRequest(
            @AuthenticationPrincipal User user,
            @RequestBody SendFriendRequestDto body) {
        FriendRequestDto dto = contactService.sendRequest(user, body.toUsername(), body.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestDto> getIncomingRequests(@AuthenticationPrincipal User user) {
        return contactService.getIncomingRequests(user);
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<Void> acceptRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        contactService.acceptRequest(user, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<Void> rejectRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        contactService.rejectRequest(user, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<FriendDto> getFriends(@AuthenticationPrincipal User user) {
        return contactService.getFriends(user);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal User user,
            @PathVariable Long userId) {
        contactService.removeFriend(user, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(
            @AuthenticationPrincipal User user,
            @PathVariable Long userId) {
        contactService.banUser(user, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/ban")
    public ResponseEntity<Void> unbanUser(
            @AuthenticationPrincipal User user,
            @PathVariable Long userId) {
        contactService.unbanUser(user, userId);
        return ResponseEntity.noContent().build();
    }
}
