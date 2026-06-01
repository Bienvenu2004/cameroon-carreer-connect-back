package com.hostdesign24.jobportal.ai.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * AI module wiring.
 *
 *   - Binds {@link AiProperties} so the {@code app.ai.*} YAML block is
 *     available as a typed dependency.
 *   - Provides a single shared {@link HttpClient} for the Gemini calls.
 *     One client is sufficient: java.net.http.HttpClient is thread-safe,
 *     reuses connections, and supports HTTP/2 by default.
 *   - Exposes a dedicated {@link ObjectMapper} bean named "aiObjectMapper"
 *     so we don't accidentally pollute the global mapper with config
 *     tuned for LLM I/O.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /** Shared HTTP/2 client. 10s connect timeout; per-request read timeout set per call. */
    @Bean
    public HttpClient aiHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Dedicated mapper so future LLM-specific tweaks (e.g. handling
     * camelCase fields from Gemini's payloads) don't leak into the
     * application-wide mapper.
     *
     * {@code FAIL_ON_UNKNOWN_PROPERTIES = false} is essential here: the
     * Gemini response carries many fields we don't bind ({@code
     * modelVersion}, {@code responseId}, {@code promptFeedback}, per-
     * candidate {@code safetyRatings}, etc.). Failing on those would
     * break every single call — and we don't want to chase the upstream
     * schema every time Google adds a field.
     */
    @Bean(name = "aiObjectMapper")
    public ObjectMapper aiObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
