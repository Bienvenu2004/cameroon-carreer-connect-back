package com.hostdesign24.jobportal.security.listener;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.services.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {
  private final LoginAttemptService loginAttemptService;

  @Override
  public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
    final String ip = Utils.getClientIp();
    if (ip != null) {
      loginAttemptService.loginFailed(ip);
    }
  }
}
