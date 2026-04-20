package com.chatapp.message;

import com.chatapp.message.dto.ChatMessageEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {

    @Nullable
    private final RabbitTemplate rabbitTemplate;

    public MessagePublisher(@Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRoomMessage(ChatMessageEvent event) {
        if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, "chat.room." + event.roomId(), event);
        }
    }

    public void publishDmMessage(ChatMessageEvent event) {
        if (rabbitTemplate != null) {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, "chat.dm", event);
        }
    }
}
