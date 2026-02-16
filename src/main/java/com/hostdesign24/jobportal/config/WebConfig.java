package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.config.converters.StringToEnumConverterFactory;
import com.hostdesign24.jobportal.security.interceptor.RateLimitingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final RateLimitingInterceptor rateLimitingInterceptor;
  @Value("${app.client-url}")
  private String clientUrl;

  public WebConfig(RateLimitingInterceptor rateLimitingInterceptor) {
    this.rateLimitingInterceptor = rateLimitingInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:3001",
                clientUrl,
                "https://localhost/"
        )
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new StringToEnumConverterFactory());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitingInterceptor)
        .addPathPatterns("/api/v1/**");
  }

}
