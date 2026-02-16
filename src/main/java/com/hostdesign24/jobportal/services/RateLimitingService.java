package com.hostdesign24.jobportal.services;

import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.UUID;

public interface RateLimitingService {
  Bucket resolveBucket(String key);
  Bucket newBucket(String key);
  boolean isPasswordResetTooFrequent(UUID userId, int maxRequests);

    boolean isEmailVerificationTooFrequent(UUID userId, int maxRequests);

    boolean isRegistrationInitiationTooFrequent(String email, int maxRequests);

    boolean isRegistrationCompletionTooFrequent(String email, int maxAttempts, Duration window);

    void clearRegistrationRateLimit(String email);
}
