package com.chatapp.attachment;

import com.chatapp.attachment.dto.AttachmentDto;
import com.chatapp.attachment.entity.Attachment;
import com.chatapp.attachment.repository.AttachmentRepository;
import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.message.dto.MessageDto;
import com.chatapp.message.entity.Message;
import com.chatapp.message.repository.MessageRepository;
import com.chatapp.room.repository.RoomMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    @Value("${app.uploads.path:/app/uploads}")
    private String uploadsPath;

    private final AttachmentRepository attachmentRepository;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final AttachmentAccessGuard accessGuard;
    private final SimpMessagingTemplate messagingTemplate;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             MessageRepository messageRepository,
                             RoomMemberRepository roomMemberRepository,
                             FriendshipRepository friendshipRepository,
                             AttachmentAccessGuard accessGuard,
                             SimpMessagingTemplate messagingTemplate) {
        this.attachmentRepository = attachmentRepository;
        this.messageRepository = messageRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.friendshipRepository = friendshipRepository;
        this.accessGuard = accessGuard;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public AttachmentDto upload(User uploader, MultipartFile file,
                                Long roomId, Long recipientId,
                                String content, Long replyToId) {
        if (roomId == null && recipientId == null) {
            throw new BadRequestException("roomId or recipientId is required");
        }
        if (roomId != null && recipientId != null) {
            throw new BadRequestException("Specify roomId or recipientId, not both");
        }
        if (roomId != null && !roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, uploader.getId())) {
            throw new ForbiddenException("Not a member of this room");
        }
        if (recipientId != null && !friendshipRepository.existsBetween(uploader.getId(), recipientId)) {
            throw new ForbiddenException("Not friends with this user");
        }

        validateFileSize(file);

        String storedName = storeFile(file);

        Message message = new Message();
        message.setRoomId(roomId);
        message.setSender(uploader);
        message.setRecipientId(recipientId);
        message.setReplyToId(replyToId);
        message.setContent(content != null && !content.isBlank() ? content.trim() : "");
        Message savedMessage = messageRepository.save(message);

        Attachment attachment = new Attachment();
        attachment.setMessage(savedMessage);
        attachment.setUploader(uploader);
        attachment.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        attachment.setStoredName(storedName);
        attachment.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setSizeBytes(file.getSize());
        attachment.setComment(content != null && !content.isBlank() ? content.trim() : null);
        Attachment saved = attachmentRepository.save(attachment);

        AttachmentDto attachmentDto = AttachmentDto.from(saved);
        MessageDto messageDto = MessageDto.from(savedMessage, List.of(attachmentDto));
        broadcast(messageDto);

        return attachmentDto;
    }

    public record DownloadResult(Resource resource, String filename, String contentType) {}

    @Transactional(readOnly = true)
    public DownloadResult download(User requester, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BadRequestException("Attachment not found"));

        accessGuard.checkDownloadAccess(requester.getId(), attachment);

        Path path = Paths.get(uploadsPath).resolve(attachment.getStoredName()).normalize();
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new BadRequestException("File not found on disk");
            }
            return new DownloadResult(resource, attachment.getFilename(), attachment.getContentType());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    public List<AttachmentDto> findByMessageIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) return List.of();
        return attachmentRepository.findByMessage_IdIn(messageIds).stream()
                .map(AttachmentDto::from)
                .toList();
    }

    private void validateFileSize(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String ct = file.getContentType();
        boolean isImage = ct != null && ct.startsWith("image/");
        long limit = isImage ? MAX_IMAGE_BYTES : MAX_FILE_BYTES;
        if (file.getSize() > limit) {
            throw new BadRequestException(isImage
                    ? "Image exceeds 3 MB limit"
                    : "File exceeds 20 MB limit");
        }
    }

    private String storeFile(MultipartFile file) {
        String month = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Path dir = Paths.get(uploadsPath, month);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : "";
        String relativePath = month + "/" + UUID.randomUUID() + ext;

        try {
            file.transferTo(Paths.get(uploadsPath, relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        return relativePath;
    }

    private void broadcast(MessageDto dto) {
        if (dto.roomId() != null) {
            messagingTemplate.convertAndSend("/topic/room." + dto.roomId(), dto);
        } else {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.senderId()), "/queue/user." + dto.senderId(), dto);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.recipientId()), "/queue/user." + dto.recipientId(), dto);
        }
    }
}
