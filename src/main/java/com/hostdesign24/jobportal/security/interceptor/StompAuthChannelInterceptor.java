package com.hostdesign24.jobportal.security.interceptor;

import com.hostdesign24.jobportal.common.utils.TokenResolver;
import com.hostdesign24.jobportal.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {
  private final JwtService jwtService;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel){
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

      String token = TokenResolver.resolveFromStomp(accessor)
          .orElse(
              (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("access_token")
              );

      if (!jwtService.validateAccessToken(token)) {
        throw new SecurityException("Invalid token");
      }

      String userId = jwtService.getUserIdFromJwtToken(token);

      Authentication user = new UsernamePasswordAuthenticationToken(
          userId, null, Collections.emptyList());

      accessor.setUser(user);
    }
    return message;
  }

}
