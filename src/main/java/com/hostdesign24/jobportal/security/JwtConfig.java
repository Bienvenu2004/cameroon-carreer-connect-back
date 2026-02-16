package com.hostdesign24.jobportal.security;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtConfig {
    private String secret;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
    private long maxRefreshAge;
}
