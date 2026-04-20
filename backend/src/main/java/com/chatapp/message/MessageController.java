package com.chatapp.message;

import com.chatapp.auth.entity.User;
import com.chatapp.message.dto.MessageDto;
import com.chatapp.message.dto.MessagePage;
import com.chatapp.message.dto.UnreadCountDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/rooms/{roomId}/messages")
    public MessagePage getRoomHistory(@AuthenticationPrincipal User user,
                                      @PathVariable Long roomId,
                                      @RequestParam(required = false) Long before) {
        return messageService.getRoomHistory(user, roomId, before);
    }

    @GetMapping("/messages/dm/{partnerId}")
    public MessagePage getDmHistory(@AuthenticationPrincipal User user,
                                    @PathVariable Long partnerId,
                                    @RequestParam(required = false) Long before) {
        return messageService.getDmHistory(user, partnerId, before);
    }

    @PatchMapping("/messages/{id}")
    public MessageDto editMessage(@AuthenticationPrincipal User user,
                                  @PathVariable Long id,
                                  @RequestBody Map<String, String> body) {
        return messageService.editMessage(user, id, body.get("content"));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@AuthenticationPrincipal User user,
                                              @PathVariable Long id) {
        messageService.deleteMessage(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rooms/{roomId}/messages/read")
    public ResponseEntity<Void> markRoomRead(@AuthenticationPrincipal User user,
                                             @PathVariable Long roomId) {
        messageService.markRoomRead(user, roomId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/dm/{partnerId}/read")
    public ResponseEntity<Void> markDmRead(@AuthenticationPrincipal User user,
                                           @PathVariable Long partnerId) {
        messageService.markDmRead(user, partnerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages/unread")
    public List<UnreadCountDto> getUnreadCounts(@AuthenticationPrincipal User user) {
        return messageService.getUnreadCounts(user);
    }
}
