package com.hostdesign24.jobportal.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {
  private static final int MAX_ATTEMPTS = 5;
  private final LoadingCache<String, Integer> attemptsCache;


  public LoginAttemptService() {
    attemptsCache = CacheBuilder.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Integer>() {
          @Override
          public Integer load(String key) throws Exception {
            return 0;
          }
        });
  }

  public void loginSucceeded(String key) {
    attemptsCache.invalidate(key);
  }

  public void loginFailed(String key) {
    try {
      int attempts = attemptsCache.get(key, () -> 0);
      attemptsCache.put(key, attempts + 1);
    } catch (ExecutionException e) {
      attemptsCache.put(key, 1);
    }
  }

  public boolean isBlocked(String key) {
    return attemptsCache.getUnchecked(key) >= MAX_ATTEMPTS;
  }
}
