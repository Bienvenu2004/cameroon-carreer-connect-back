package com.hostdesign24.jobportal.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TokenResolver {

  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ACCESS_TOKEN_COOKIE = "access_token";

  /**
   * Resolve token from HttpServletRequest (used in REST filters).
   */
  public static Optional<String> resolveFromHttpRequest(HttpServletRequest request){
    String bearerToken = request.getHeader(AUTH_HEADER);
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
    }

    if (request.getCookies() != null) {
      return Arrays.stream(request.getCookies())
          .filter(cookie -> ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
          .map(Cookie::getValue)
          .filter(StringUtils::hasText)
          .findFirst();
    }

    return Optional.empty();
  }

  /**
   * Resolve token from STOMP headers (used in WebSocket interceptor).
   */
  public static Optional<String> resolveFromStomp(StompHeaderAccessor accessor){
    List<String> auth = accessor.getNativeHeader(AUTH_HEADER);
    if (auth != null && !auth.isEmpty()) {
      String token = auth.getFirst();

      if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
        return Optional.of(token.substring(BEARER_PREFIX.length()));
      }
    }

    return Optional.empty();
  }

  private TokenResolver() {}
}
