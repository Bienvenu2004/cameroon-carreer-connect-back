package com.hostdesign24.jobportal.security.interceptor;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.services.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.common.lang.NonNullApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
@NonNullApi
public class RateLimitingInterceptor implements HandlerInterceptor {

  private final RateLimitingService rateLimitingService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String key = resolveKey();

    Bucket bucket = rateLimitingService.resolveBucket(key);
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      return true;
    } else {
      long nanosToWaitForRefill = probe.getNanosToWaitForRefill();
      long secondsToWait = (long) Math.ceil((double) nanosToWaitForRefill / 1_000_000_000);

      response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(secondsToWait));
      response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
          "You have exhausted your API request quota. Please try again later.");
      return false;
    }
  }

  private String resolveKey() {
    var currentUser = Utils.getCurrentUser().orElse(null);

    if (currentUser != null) {
      return currentUser.getId().toString();
    }
    log.info("this is the users current ip" + Utils.getClientIp());
    return Utils.getClientIp();
  }
}
