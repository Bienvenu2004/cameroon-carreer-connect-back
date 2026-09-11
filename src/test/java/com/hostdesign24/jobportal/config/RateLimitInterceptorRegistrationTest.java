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

    private static final String JOB = "b011c7f7-8f8c-4d10-903c-f34bd5689eca";
    private static final String COMPANY = "3f2c1a90-5d4e-4b7a-9c61-0e8a2b7d4f15";

    /**
     * Whether a request spends a rate-limit token: the interceptor must be mapped to
     * its path, and the request must not be one of the exempt public reads. Both
     * halves are the production rule, not a re-implementation of it.
     */
    private boolean throttles(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        // MappedInterceptor reads the parsed RequestPath that DispatcherServlet
        // normally caches on the request; outside a dispatch we parse it ourselves.
        ServletRequestPathUtils.parseAndCache(request);
        return mapped.getFirst().matches(request)
                && !com.hostdesign24.jobportal.security.interceptor.RateLimitExemptions.isExempt(request);
    }

    @Test
    @DisplayName("business endpoints are throttled, including writes on publicly readable paths")
    void throttlesApplicationEndpoints() {
        assertThat(throttles("POST", "/api/hjp/auth/login")).isTrue();
        assertThat(throttles("POST", "/api/hjp/jobs/apply")).isTrue();
        assertThat(throttles("PATCH", "/api/hjp/admin/users/1/suspend")).isTrue();
        assertThat(throttles("GET", "/api/hjp/notifications")).isTrue();

        // Being public to read must not make writing free. Before the exemption became
        // method-aware, every one of these company writes escaped the limiter.
        assertThat(throttles("POST", "/api/hjp/companies/")).as("create a company").isTrue();
        assertThat(throttles("PATCH", "/api/hjp/companies/" + COMPANY)).as("edit a company").isTrue();
        assertThat(throttles("DELETE", "/api/hjp/companies/" + COMPANY)).as("delete a company").isTrue();
        assertThat(throttles("POST", "/api/hjp/companies/" + COMPANY + "/follow")).as("follow a company").isTrue();
        assertThat(throttles("PATCH", "/api/hjp/jobs/" + JOB)).as("edit a job").isTrue();
        assertThat(throttles("POST", "/api/hjp/jobs/" + JOB + "/report")).as("report a job").isTrue();

        // A signed-in user's own data is not public browsing, even where it fits /jobs/{id}.
        assertThat(throttles("GET", "/api/hjp/jobs/applications")).isTrue();
        assertThat(throttles("GET", "/api/hjp/companies/me")).isTrue();
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
    void excludesPublicBrowsing() throws Exception {
        assertThat(throttles("GET", "/api/hjp/jobs/all")).isFalse();
        assertThat(throttles("GET", "/api/hjp/jobs/search")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/industry-counts")).isFalse();

        // Opening a single offer or employer is browsing too. These were throttled
        // before, so visitors sharing a mobile IP could be locked out of job pages.
        assertThat(throttles("GET", "/api/hjp/jobs/" + JOB)).as("open a job").isFalse();
        assertThat(throttles("GET", "/api/hjp/jobs/" + JOB + "/similar")).isFalse();
        assertThat(throttles("GET", "/api/hjp/jobs/" + JOB + "/company-jobs")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/" + COMPANY)).as("open a company").isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/" + COMPANY + "/responsiveness")).isFalse();
        assertThat(throttles("GET", "/api/hjp/companies/" + COMPANY + "/followers/count")).isFalse();

        // The interceptor itself must apply the exemption before touching a bucket: it
        // is built here with no rate-limiting service, so reaching one would throw.
        MockHttpServletRequest view = new MockHttpServletRequest("GET", "/api/hjp/jobs/" + JOB);
        assertThat(new RateLimitingInterceptor(null).preHandle(
                view, new org.springframework.mock.web.MockHttpServletResponse(), new Object()))
                .isTrue();
    }

    @Test
    @DisplayName("file streaming and the websocket handshake are outside the mapping")
    void leavesNonApiPathsAlone() {
        assertThat(throttles("GET", "/storage/some-file-id")).isFalse();
        assertThat(throttles("GET", "/retms-websocket/info")).isFalse();
    }
}
