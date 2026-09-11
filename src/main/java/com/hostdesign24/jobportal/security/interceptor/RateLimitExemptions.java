package com.hostdesign24.jobportal.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;

/**
 * The requests the rate limiter lets through without spending a token.
 *
 * <p>Exempt: anonymous browsing -- listing, searching and opening job and company
 * pages. Buckets for unauthenticated callers are keyed by IP address, and behind
 * the carrier-grade NAT common on Cameroonian mobile networks one address is
 * shared by many genuine visitors, so throttling these reads would lock out
 * everyone on that address because one of them scrolled quickly.
 *
 * <p>The exemption is deliberately narrower than a path prefix, on two axes:
 * <ul>
 *   <li><b>Method.</b> Only GET is exempt. The limiter used to be registered with
 *       {@code excludePathPatterns("/api/hjp/companies/**")}, which exempted every
 *       method -- so creating, editing, deleting and following companies escaped
 *       throttling entirely, while {@code GET /api/hjp/jobs/{id}}, a plain page
 *       view, did not. A path pattern cannot tell a read from a write; this can.</li>
 *   <li><b>Shape.</b> Identifiers must look like UUIDs, so {@code /jobs/applications}
 *       and {@code /companies/me} -- a signed-in user's own data -- are not taken for
 *       public pages merely because they fit {@code /jobs/{something}}.</li>
 * </ul>
 *
 * <p>The list mirrors the GET routes {@code SecurityConfig} opens to anonymous
 * callers. A route made public there but missing here is merely throttled; a
 * route listed here but not public there is rejected by security before the
 * limiter runs. Neither mistake leaves a write unthrottled.
 */
public final class RateLimitExemptions {

    /** A UUID's five hex groups. PathPattern regexes cannot contain braces, hence no {8}. */
    private static final String ID =
            "{id:[0-9a-fA-F]+-[0-9a-fA-F]+-[0-9a-fA-F]+-[0-9a-fA-F]+-[0-9a-fA-F]+}";

    private static final List<PathPattern> PUBLIC_READS = compile(
            "/api/hjp/jobs/all",
            "/api/hjp/jobs/search",
            "/api/hjp/jobs/" + ID,
            "/api/hjp/jobs/" + ID + "/similar",
            "/api/hjp/jobs/" + ID + "/company-jobs",
            "/api/hjp/companies",
            "/api/hjp/companies/",
            "/api/hjp/companies/industry-counts",
            "/api/hjp/companies/" + ID,
            "/api/hjp/companies/" + ID + "/responsiveness",
            "/api/hjp/companies/" + ID + "/followers/count");

    private RateLimitExemptions() {
    }

    public static boolean isExempt(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return isExempt(request.getMethod(), path);
    }

    public static boolean isExempt(String method, String path) {
        if (!HttpMethod.GET.matches(method)) {
            return false;
        }
        PathContainer container = PathContainer.parsePath(path);
        return PUBLIC_READS.stream().anyMatch(pattern -> pattern.matches(container));
    }

    private static List<PathPattern> compile(String... patterns) {
        PathPatternParser parser = new PathPatternParser();
        return Arrays.stream(patterns).map(parser::parse).toList();
    }
}
