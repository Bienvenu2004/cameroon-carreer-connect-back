package com.hostdesign24.jobportal.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistService {
  private final Cache<String, Boolean> blacklist;

  public JwtBlacklistService() {
    this.blacklist = CacheBuilder.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build();
  }

  public void blacklist(String jti) {
    if (jti != null) {
      blacklist.put(jti, true);
    }
  }

  public boolean isBlacklisted(String jti) {
    return jti != null && blacklist.getIfPresent(jti) != null;
  }
}
