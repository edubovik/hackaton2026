package com.chatapp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Wraps SimpMessagingTemplate to suppress MessageDeliveryException when the
 * STOMP broker relay (RabbitMQ) is temporarily unavailable — e.g. in tests.
 * Business logic still completes; real-time delivery is best-effort.
 */
@Component
public class BrokerTemplate {

    private static final Logger log = LoggerFactory.getLogger(BrokerTemplate.class);

    private final SimpMessagingTemplate delegate;

    public BrokerTemplate(SimpMessagingTemplate delegate) {
        this.delegate = delegate;
    }

    public void send(String destination, Object payload) {
        try {
            delegate.convertAndSend(destination, payload);
        } catch (MessagingException e) {
            log.warn("Broker unavailable, notification dropped for {}: {}", destination, e.getMessage());
        }
    }

    public void sendToUser(String user, String destination, Object payload) {
        try {
            delegate.convertAndSendToUser(user, destination, payload);
        } catch (MessagingException e) {
            log.warn("Broker unavailable, notification dropped for user {}: {}", user, e.getMessage());
        }
    }
}
