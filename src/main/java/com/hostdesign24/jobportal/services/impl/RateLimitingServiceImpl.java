package com.hostdesign24.jobportal.services.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.hostdesign24.jobportal.services.RateLimitingService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private final LoadingCache<String, Bucket> cache;
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();


    public RateLimitingServiceImpl() {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @Override
                    public Bucket load(String key) {
                        return newBucket(key);
                    }
                });
    }

    @Override
    public Bucket resolveBucket(String key) {
        return cache.getUnchecked(key);
    }

    @Override
    public Bucket newBucket(String key) {
        // Default limit of 120 request per minute
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofMinutes(1))
                .build();

        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public boolean isPasswordResetTooFrequent(UUID userId, int maxRequests) {
        String key = "password_reset:" + userId;
        log.debug("Checking rate limit for key: {}", key);

        Bucket bucket = bucketCache.computeIfAbsent(key, k -> {
            log.info("Creating new bucket for key: {}", k);
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(
                            maxRequests,
                            Refill.intervally(maxRequests, Duration.ofHours(1))
                    ))
                    .build();
        });
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            log.debug("Request allowed for {}. Remaining tokens: {}",
                    key, probe.getRemainingTokens());
            return false;
        } else {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit EXCEEDED for {}. Wait {} seconds",
                    key, secondsToWait);
            return true;
        }
    }

    /**
     * Check if email verification resend is too frequent
     */
    @Override
    public boolean isEmailVerificationTooFrequent(UUID userId, int maxRequests) {
        String key = "email_verification:" + userId;

        Bucket bucket = bucketCache.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                maxRequests,
                                Refill.intervally(maxRequests, Duration.ofHours(1))
                        ))
                        .build()
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            log.warn("Email verification rate limit exceeded for key: {}", key);
            return true;
        }

        return false;
    }

    /**
     * Check if registration initiation is too frequent
     * Uses email as key since user doesn't exist yet
     * Allows maxRequests per hour per email
     *
     * @param email Email address attempting registration
     * @param maxRequests Maximum requests allowed per hour (recommended: 3)
     * @return true if rate limit exceeded, false if allowed
     */
    @Override
    public boolean isRegistrationInitiationTooFrequent(String email, int maxRequests) {
        // Normalize email (lowercase, trim)
        String normalizedEmail = email.toLowerCase().trim();
        String key = "registration_init:" + normalizedEmail;

        // Get or create bucket for this email
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> {
            log.debug("Creating new registration rate limit bucket for email: {}", normalizedEmail);
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(
                            maxRequests,  // e.g., 3 requests
                            Refill.intervally(maxRequests, Duration.ofHours(1)) // per hour
                    ))
                    .build();
        });

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Registration initiation rate limit exceeded for email: {}. Retry after {} seconds",
                    normalizedEmail, secondsToWait);
            return true; // Rate limit exceeded
        }

        log.debug("Registration initiation allowed for email: {}. Tokens remaining: {}",
                normalizedEmail, probe.getRemainingTokens());
        return false; // Allowed
    }

    /**
     * Check if registration completion attempts are too frequent
     * Used to prevent brute force of verification codes
     *
     * @param email Email address
     * @param maxAttempts Maximum attempts allowed in time window (recommended: 10)
     * @param window Time window (recommended: 5 minutes)
     * @return true if rate limit exceeded, false if allowed
     */
    @Override
    public boolean isRegistrationCompletionTooFrequent(String email, int maxAttempts, Duration window) {
        String normalizedEmail = email.toLowerCase().trim();
        String key = "registration_complete:" + normalizedEmail;

        Bucket bucket = bucketCache.computeIfAbsent(key, k -> {
            log.debug("Creating verification attempt rate limit bucket for email: {}", normalizedEmail);
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(
                            maxAttempts,  // e.g., 10 attempts
                            Refill.intervally(maxAttempts, window) // e.g., per 5 minutes
                    ))
                    .build();
        });

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Registration completion rate limit exceeded for email: {}. Retry after {} seconds",
                    normalizedEmail, secondsToWait);
            return true;
        }

        log.debug("Registration completion attempt allowed for email: {}. Attempts remaining: {}",
                normalizedEmail, probe.getRemainingTokens());
        return false;
    }

    /**
     * Clear rate limit for specific email (use after successful registration)
     */
    @Override
    public void clearRegistrationRateLimit(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        bucketCache.remove("registration_init:" + normalizedEmail);
        bucketCache.remove("registration_complete:" + normalizedEmail);
        log.debug("Cleared registration rate limits for email: {}", normalizedEmail);
    }
}

