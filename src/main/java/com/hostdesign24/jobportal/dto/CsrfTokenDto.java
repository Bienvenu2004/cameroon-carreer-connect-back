package com.hostdesign24.jobportal.dto;

/**
 * CSRF token handed to the SPA by {@code GET /api/hjp/auth/csrf}.
 *
 * @param token      the token value to echo back on state-changing requests
 * @param headerName the header it must be sent in (X-XSRF-TOKEN by default)
 */
public record CsrfTokenDto(String token, String headerName) {
}
