package com.hostdesign24.jobportal.security;

import com.hostdesign24.jobportal.common.utils.TokenResolver;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.UserRepository;
import io.micrometer.common.lang.NonNullApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
@NonNullApi
public class JwtFilters extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/",
            "/error",
            "/actuator/health"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Check if path matches any public path
        boolean isPublic = PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith);

        if (isPublic) {
            log.debug("Skipping JWT filter for public path: {}", path);
        }

        return isPublic;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final Optional<String> jwtOptional = TokenResolver.resolveFromHttpRequest(request);

        if (jwtOptional.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = jwtOptional.get();

        try {
            if (!jwtService.validateAccessToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String userId = jwtService.getUserIdFromJwtToken(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
                if (user == null || jwtService.userTokenIsInvalid(jwt, user)) {
                    log.warn("Token validation failed for user: {}", userId);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("Token is no longer valid");
                    return;
                }

                Collection<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (Exception e) {
            log.error("JWT Authentication failed", e);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
