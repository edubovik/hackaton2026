package com.chatapp.attachment;

import com.chatapp.attachment.repository.AttachmentRepository;
import com.chatapp.auth.entity.User;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.message.repository.MessageRepository;
import com.chatapp.room.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.chatapp.common.BrokerTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock MessageRepository messageRepository;
    @Mock RoomMemberRepository roomMemberRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock AttachmentAccessGuard accessGuard;
    @Mock BrokerTemplate messagingTemplate;

    @TempDir Path tempDir;

    AttachmentService service;

    User uploader;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(
                attachmentRepository, messageRepository, roomMemberRepository,
                friendshipRepository, accessGuard, messagingTemplate);
        ReflectionTestUtils.setField(service, "uploadsPath", tempDir.toString());

        uploader = new User();
        ReflectionTestUtils.setField(uploader, "id", 1L);
    }

    @Test
    void upload_noContext_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.upload(uploader, file, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("roomId or recipientId");
    }

    @Test
    void upload_bothContexts_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> service.upload(uploader, file, 1L, 2L, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not both");
    }

    @Test
    void upload_emptyFile_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt",
                "text/plain", new byte[0]);
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(uploader, file, 1L, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void upload_imageTooLarge_throwsBadRequest() {
        byte[] bigImage = new byte[4 * 1024 * 1024]; // 4 MB > 3 MB limit
        MockMultipartFile file = new MockMultipartFile("file", "big.png",
                "image/png", bigImage);
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(uploader, file, 1L, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("3 MB");
    }

    @Test
    void upload_fileTooLarge_throwsBadRequest() {
        byte[] bigFile = new byte[21 * 1024 * 1024]; // 21 MB > 20 MB limit
        MockMultipartFile file = new MockMultipartFile("file", "big.zip",
                "application/zip", bigFile);
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.upload(uploader, file, 1L, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("20 MB");
    }

    @Test
    void upload_notRoomMember_throwsForbidden() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt",
                "text/plain", "content".getBytes());
        when(roomMemberRepository.existsByRoom_IdAndUser_Id(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(uploader, file, 1L, null, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void upload_dmNotFriend_throwsForbidden() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt",
                "text/plain", "content".getBytes());
        when(friendshipRepository.existsBetween(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.upload(uploader, file, null, 99L, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void download_attachmentNotFound_throwsBadRequest() {
        when(attachmentRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.download(uploader, 999L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found");
    }
}
