package com.chatapp.message;

import com.chatapp.message.dto.ChatMessageEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public MessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRoomMessage(ChatMessageEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, "chat.room." + event.roomId(), event);
    }

    public void publishDmMessage(ChatMessageEvent event) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, "chat.dm", event);
    }
}
