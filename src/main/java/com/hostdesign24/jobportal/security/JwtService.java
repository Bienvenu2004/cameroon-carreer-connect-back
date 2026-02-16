package com.hostdesign24.jobportal.security;

import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.services.JwtBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Service for issuing and validating JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

  private final JwtConfig jwtConfig;
  private final JwtBlacklistService blacklistService;

  public String generateAccessToken(User user) {
    return generateToken(user, jwtConfig.getAccessTokenExpiration(), "access");
  }

  public String generateRefreshToken(User user) {
    return generateToken(user, jwtConfig.getRefreshTokenExpiration(), "refresh");
  }

  private String generateToken(User user, long tokenExpiration, String tokenType) {
    long now = System.currentTimeMillis();
    JwtBuilder builder = Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole())
        .claim("jti", UUID.randomUUID().toString())
        .claim("type", tokenType)
        .claim("gen_time", now)
        .issuedAt(new Date(now))
        .expiration(new Date(now + tokenExpiration))
        .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)));

    if (user.getPasswordChangedAt() != null) {
      builder.claim("pwd_changed", user.getPasswordChangedAt().getTime());
    }

    try {
      return builder.compact();
    } catch (Exception e) {
      log.error("Token generation failed", e);
      return null;
    }

  }

  private Claims getClaims(String token) {
    return Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

    public boolean userTokenIsInvalid(String token, User user) {
        try {
            Claims claims = getClaims(token);

            Long tokenPwdChanged = claims.get("pwd_changed", Long.class);

            // Get current password change time from DB
            Long userPwdChanged = user.getPasswordChangedAt() != null
                    ? user.getPasswordChangedAt().getTime()
                    : null;

            // Case 1: Token has no pwd_changed claim (old token format)
            if (tokenPwdChanged == null) {
                log.warn("Token without pwd_changed claim detected for user: {}", user.getId());
                // Reject old tokens if user has ever changed password
                if (userPwdChanged != null) {
                    log.info("Token invalid: old token format with password change history");
                    return true;
                }
                // Allow only if user has never changed password
                return false;
            }

            // Case 2: Token has pwd_changed, but user never changed password
            if (userPwdChanged == null) {
                log.warn("Token has pwd_changed but user has no password change history");
                return true; // Suspicious - reject
            }

            // Case 3: Both exist - compare timestamps
            if (!tokenPwdChanged.equals(userPwdChanged)) {
                log.info("Token invalid: password changed after token creation. Token: {}, Current: {}",
                        tokenPwdChanged, userPwdChanged);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Token validation against user failed", e);
            return true;
        }
    }

  public boolean validateRefreshToken(String token) {
    try {
      Claims claims = getClaims(token);
      if (!"refresh".equals(claims.get("type", String.class))) {
        log.warn("Token type is not 'refresh'");
        return false;
      }

      if (!isTokenUnexpired(claims)) {
        return false;
      }

      Long genTime = claims.get("gen_time", Long.class);
      if (genTime == null) {
        return false;
      }
      long maxRefreshAge = jwtConfig.getMaxRefreshAge();
      log.debug("Max refresh age: {}", maxRefreshAge);

      return (System.currentTimeMillis() - genTime) < maxRefreshAge;
    } catch (JwtException e) {
      log.error("Token validation failed", e);
      return false;
    }
  }


  public boolean validateAccessToken(String token) {
    try {
      Claims claims = getClaims(token);
      String jti = claims.getId();

      if (blacklistService.isBlacklisted(jti)) {
        log.warn("Attempted to use a blacklisted (logged out) JWT: {}", jti);
        return false;
      }

      if (!"access".equals(claims.get("type", String.class))) {
        log.warn("Token type is not 'access'");
        return false;
      }

      return isTokenUnexpired(claims);
    } catch (JwtException e) {
      log.error("Token validation failed", e);
      return false;
    }
  }


  public String getUserIdFromJwtToken(String token) {
    return getClaims(token).getSubject();
  }

  public String getTokenId(String token) {
    return getClaims(token).getId();
  }

  private boolean isTokenUnexpired(Claims claims) {
    return claims.getExpiration().after(new Date());
  }

}
