package com.hostdesign24.jobportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
    private final ObjectMapper objectMapper;

    @Value("${app.cookies.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookies.same-site:Lax}")
    private String cookieSameSite;

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
        // ------------------------------------------------------------------
        // CSRF
        //
        // This API authenticates with an HttpOnly cookie, so the browser
        // attaches credentials to cross-site requests automatically. CSRF
        // protection was previously disabled outright, which is only safe for
        // APIs that authenticate from a header the attacker cannot set.
        //
        // We use the double-submit cookie pattern: the token is written to a
        // JS-readable XSRF-TOKEN cookie and must be echoed back in the
        // X-XSRF-TOKEN header. Setting the request-attribute name to null opts
        // out of Spring's deferred token loading so the cookie is written on
        // every response, including the GET /api/hjp/auth/csrf seeding call the
        // SPA makes at startup.
        //
        // The SockJS transport is excluded: its fallback transports POST to
        // /retms-websocket and the STOMP channel carries its own authentication.
        // ------------------------------------------------------------------
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();
        csrfRepository.setCookieCustomizer(cookie -> cookie
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/"));

        http.sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .ignoringRequestMatchers("/retms-websocket/**"))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authorizeRequests ->
                                authorizeRequests
                                        .requestMatchers(HttpMethod.POST, "/api/hjp/auth/**")
                                        .permitAll()
                                        // Registration form checks whether an email is taken.
                                        // The auth wildcard above only covers POST, so this GET
                                        // needs its own rule -- and it must carry the real path.
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/auth/validate-email/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/auth/csrf")
                                        .permitAll()
                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                        .permitAll()
                                        // Must match the endpoint registered in WebSocketConfig.
                                        .requestMatchers("/retms-websocket/**")
                                        .permitAll()
                                        .requestMatchers("/error").permitAll()
                                        .requestMatchers("/storage/**").permitAll()
                                        // Public job browsing — home page, job listings, job detail
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/jobs/all").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/jobs/search").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/jobs/*").permitAll()
                                        // Public company browsing — company list and detail
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/companies", "/api/hjp/companies/").permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/hjp/companies/*").permitAll()
                                        // AI semantic job search (§5.2) — public, anonymous-friendly
                                        .requestMatchers(HttpMethod.POST, "/api/hjp/ai/search").permitAll()
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

                                        objectMapper.writeValue(response.getOutputStream(), body);
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

                                        objectMapper.writeValue(response.getOutputStream(), body);
                                    });
                        });
        return http.build();
    }
}
