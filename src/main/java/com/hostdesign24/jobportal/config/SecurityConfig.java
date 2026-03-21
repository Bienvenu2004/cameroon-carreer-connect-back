package com.hostdesign24.jobportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.security.JwtFilters;
import com.hostdesign24.jobportal.security.SpringSecurityAuditorAwareImpl;
import com.hostdesign24.jobportal.services.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    public static final String MESSAGE = "message";
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtFilters jwtFilters;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return new SpringSecurityAuditorAwareImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authorizeRequests ->
                                authorizeRequests
                                        .requestMatchers(HttpMethod.POST, "/api/hjp/auth/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/v1/hjp/validate-email/**")
                                        .permitAll()
                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers("/hjp-websocket/**")
                                        .permitAll()
                                        .requestMatchers("/h2-console/**")
                                        .permitAll()
                                        .requestMatchers("/error").permitAll()
                                        .requestMatchers("/logo/**").permitAll()
                                        .requestMatchers("/storage/**").permitAll()
                                        .requestMatchers("/api/v1/contacts/request-demo").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/job-posts/").permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(jwtFilters, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exception -> {
                            exception.authenticationEntryPoint(
                                    (request, response, authException) -> {
                                        log.error(
                                                "Authentication error for {} {}: {}",
                                                request.getMethod(),
                                                request.getRequestURI(),
                                                authException.getMessage());
                                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                        Map<String, Object> body = new HashMap<>();
                                        body.put("status", HttpStatus.UNAUTHORIZED.value());

                                        if (authException instanceof BadCredentialsException) {
                                            body.put(MESSAGE, "Incorrect email or password provided");
                                        } else {
                                            body.put(MESSAGE, authException.getMessage());
                                        }
                                        body.put("path", request.getRequestURI());

                                        final ObjectMapper mapper = new ObjectMapper();
                                        mapper.writeValue(response.getOutputStream(), body);
                                    });
                            exception.accessDeniedHandler(
                                    (request, response, accessDeniedException) -> {
                                        log.error(
                                                "Access denied for {} {}: {}",
                                                request.getMethod(),
                                                request.getRequestURI(),
                                                accessDeniedException.getMessage());

                                        response.setStatus(HttpStatus.FORBIDDEN.value());
                                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                                        Map<String, Object> body = new HashMap<>();
                                        body.put("status", HttpStatus.FORBIDDEN.value());
                                        body.put("error", "Forbidden");
                                        body.put(MESSAGE, "You do not have permission to perform this action");
                                        body.put("path", request.getRequestURI());

                                        new ObjectMapper().writeValue(response.getOutputStream(), body);
                                    });
                        });
        return http.build();
    }
}
