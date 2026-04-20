package com.chatapp.message;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "chat.exchange";
    public static final String QUEUE    = "chat.messages";
    public static final String ROUTING  = "chat.#";

    @Bean
    TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue chatQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange).with(ROUTING);
    }

    @Bean
    Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
