package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.security.interceptor.RateLimitingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the rate limiter's path registration.
 *
 * The interceptor was registered on {@code /api/v1/**} while every controller in
 * this application lives under {@code /api/hjp/**}, so Bucket4j throttling never
 * ran -- silently, because a misrouted interceptor produces no error, just no
 * protection. A unit test is the only thing that catches that class of bug.
 *
 * {@code InterceptorRegistry#getInterceptors} is protected, so it is reached
 * reflectively; the assertions themselves use the public
 * {@link MappedInterceptor#matches} contract.
 */
class RateLimitInterceptorRegistrationTest {

    private List<MappedInterceptor> mapped;

    @BeforeEach
    void setUp() {
        WebConfig webConfig = new WebConfig(
                new RateLimitingInterceptor(null),
                new AllowedOrigins("https://jobconnect.cm"));

        InterceptorRegistry registry = new InterceptorRegistry();
        webConfig.addInterceptors(registry);

        List<Object> registered = ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        assertThat(registered).isNotNull();
        mapped = registered.stream()
                .filter(MappedInterceptor.class::isInstance)
                .map(MappedInterceptor.class::cast)
                .toList();

        assertThat(mapped)
                .as("the rate limiter must be registered with path mappings")
                .hasSize(1);
    }

    private boolean throttles(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        // MappedInterceptor reads the parsed RequestPath that DispatcherServlet
        // normally caches on the request; outside a dispatch we parse it ourselves.
        ServletRequestPathUtils.parseAndCache(request);
        return mapped.getFirst().matches(request);
    }

    @Test
    @DisplayName("authenticated business endpoints are throttled")
    void throttlesApplicationEndpoints() {
        assertThat(throttles("POST", "/api/hjp/auth/login")).isTrue();
        assertThat(throttles("POST", "/api/hjp/jobs/apply")).isTrue();
        assertThat(throttles("PATCH", "/api/hjp/admin/users/1/suspend")).isTrue();
        assertThat(throttles("GET", "/api/hjp/notifications")).isTrue();
    }

    @Test
    @DisplayName("the dead /api/v1 prefix is no longer what the limiter listens on")
    void doesNotOnlyCoverTheOldDeadPrefix() {
        assertThat(throttles("POST", "/api/v1/anything"))
                .as("nothing is served under /api/v1; matching only there is the original bug")
                .isFalse();
    }

    @Test
    @DisplayName("anonymous browsing is excluded so shared mobile IPs are not throttled")
    void excludesPublicBrowsing() {
        assertThat(throttles("GET", "/api/hjp/jobs/all")).isFalse();
        assertThat(throttles("GET", "/api/hjp/jobs/search")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/some-id")).isFalse();
    }

    @Test
    @DisplayName("file streaming and the websocket handshake are outside the mapping")
    void leavesNonApiPathsAlone() {
        assertThat(throttles("GET", "/storage/some-file-id")).isFalse();
        assertThat(throttles("GET", "/retms-websocket/info")).isFalse();
    }
}
