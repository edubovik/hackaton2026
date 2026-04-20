package com.chatapp.attachment;

import com.chatapp.attachment.dto.AttachmentDto;
import com.chatapp.auth.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentDto upload(@AuthenticationPrincipal User user,
                                @RequestPart("file") MultipartFile file,
                                @RequestParam(required = false) Long roomId,
                                @RequestParam(required = false) Long recipientId,
                                @RequestParam(required = false) String content,
                                @RequestParam(required = false) Long replyToId) {
        return attachmentService.upload(user, file, roomId, recipientId, content, replyToId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User user,
                                             @PathVariable Long id) {
        AttachmentService.DownloadResult result = attachmentService.download(user, id);
        String disposition = "attachment; filename=\"" + result.filename().replace("\"", "") + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.resource());
    }
}
