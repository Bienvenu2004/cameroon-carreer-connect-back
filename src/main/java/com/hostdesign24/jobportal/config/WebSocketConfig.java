package com.hostdesign24.jobportal.config;


import com.hostdesign24.jobportal.security.interceptor.StompAuthChannelInterceptor;
import com.hostdesign24.jobportal.security.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
  private final AllowedOrigins allowedOrigins;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Origins must match the REST CORS mapping. A wildcard here would let any
    // site open an authenticated socket, since the browser attaches the session
    // cookie to the handshake.
    registry.addEndpoint("/retms-websocket")
        .setAllowedOrigins(allowedOrigins.asArray())
        .addInterceptors(new WebSocketHandshakeInterceptor())
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // topic for public, broadcast messages
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthChannelInterceptor);
  }
}
