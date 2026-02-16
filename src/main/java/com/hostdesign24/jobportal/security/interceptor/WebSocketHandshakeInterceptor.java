package com.hostdesign24.jobportal.security.interceptor;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {


  @Override
  public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    if (request instanceof ServletServerHttpRequest servletRequest) {
      Cookie[] cookies = servletRequest.getServletRequest().getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("access_token".equals(cookie.getName())) {
            attributes.put("access_token", cookie.getValue());
          }
        }
      }
    }
    return true;
  }

  @Override
  public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
      WebSocketHandler wsHandler, Exception exception) {

  }
}
