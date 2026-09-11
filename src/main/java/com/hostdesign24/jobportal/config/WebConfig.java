package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.config.converters.StringToEnumConverterFactory;
import com.hostdesign24.jobportal.security.interceptor.RateLimitingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final RateLimitingInterceptor rateLimitingInterceptor;
  private final AllowedOrigins allowedOrigins;

  public WebConfig(RateLimitingInterceptor rateLimitingInterceptor,
                   AllowedOrigins allowedOrigins) {
    this.rateLimitingInterceptor = rateLimitingInterceptor;
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/hjp/**")
        .allowedOrigins(allowedOrigins.asArray())
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new StringToEnumConverterFactory());
  }

  /**
   * Bucket4j throttling.
   *
   * This was previously registered on {@code /api/v1/**}, a prefix no controller
   * in this application uses -- so the interceptor never ran. It is now bound to
   * the real {@code /api/hjp/**} prefix.
   *
   * Anonymous job and company browsing is exempt, but that exemption is applied
   * inside the interceptor rather than with excludePathPatterns here, because it
   * depends on the HTTP method and a path pattern cannot express that. Excluding
   * {@code /api/hjp/companies/**} by path exempted writes to companies as well,
   * while opening a single job page stayed throttled. See
   * {@link com.hostdesign24.jobportal.security.interceptor.RateLimitExemptions}.
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimitingInterceptor)
        .addPathPatterns("/api/hjp/**");
  }

}
