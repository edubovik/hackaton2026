package com.chatapp.message;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.message.dto.ChatMessageEvent;
import com.chatapp.message.dto.MessageDto;
import com.chatapp.message.entity.Message;
import com.chatapp.message.repository.MessageRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.chatapp.common.BrokerTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MessageListener {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final BrokerTemplate messagingTemplate;

    public MessageListener(MessageRepository messageRepository,
                           UserRepository userRepository,
                           MessageService messageService,
                           BrokerTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    @Transactional
    public void onChatMessage(ChatMessageEvent event) {
        User sender = userRepository.findById(event.senderId())
                .orElseThrow(() -> new IllegalStateException("Sender not found: " + event.senderId()));

        Message message = new Message();
        message.setSender(sender);
        message.setContent(event.content());
        message.setReplyToId(event.replyToId());

        if ("ROOM".equals(event.type())) {
            message.setRoomId(event.roomId());
        } else {
            message.setRecipientId(event.recipientId());
        }

        Message saved = messageRepository.save(message);
        MessageDto dto = MessageDto.from(saved);

        if ("ROOM".equals(event.type())) {
            messageService.incrementRoomUnread(event.roomId(), event.senderId());
            messagingTemplate.send("/topic/room." + event.roomId(), dto);
        } else {
            messageService.incrementDmUnread(event.recipientId(), event.senderId());
            messagingTemplate.send("/topic/user." + event.recipientId(), dto);
            messagingTemplate.send("/topic/user." + event.senderId(), dto);
        }
    }
}
