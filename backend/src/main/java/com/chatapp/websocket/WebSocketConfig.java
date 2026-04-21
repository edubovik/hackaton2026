package com.chatapp.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.stomp.relay-host}")
    private String relayHost;

    @Value("${app.stomp.relay-port}")
    private int relayPort;

    @Value("${app.stomp.login}")
    private String relayLogin;

    @Value("${app.stomp.passcode}")
    private String relayPasscode;

    private final WebSocketAuthInterceptor authInterceptor;
    private final CookieHandshakeInterceptor cookieHandshakeInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                           CookieHandshakeInterceptor cookieHandshakeInterceptor) {
        this.authInterceptor = authInterceptor;
        this.cookieHandshakeInterceptor = cookieHandshakeInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").addInterceptors(cookieHandshakeInterceptor).setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayLogin)
                .setClientPasscode(relayPasscode)
                .setSystemLogin(relayLogin)
                .setSystemPasscode(relayPasscode);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
