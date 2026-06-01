package com.hostdesign24.jobportal.ai.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around Google's Gemini Generative Language REST API
 * ({@code /v1beta/models/{model}:generateContent}).
 *
 * Why a hand-rolled client and not Spring AI / the official SDK?
 *   1. Zero new dependencies — Java 21's {@code java.net.http.HttpClient}
 *      handles everything we need.
 *   2. Full visibility into the request/response — every byte we send to
 *      Google is constructed in this file. Easier to audit from a privacy
 *      standpoint.
 *   3. Avoids any version-skew risk between Spring AI's GA line (targets
 *      Spring Boot 3.x) and this project's Spring Boot 4.
 *
 * Logs token usage + latency on every call, but NEVER the prompt or
 * response content — that goes through the structured-log channel only
 * when explicit debug is enabled, never to production aggregators.
 *
 * Auth model: Gemini accepts the key via the {@code X-goog-api-key}
 * request header (preferred — keeps the key out of URLs, proxy logs,
 * and browser histories). NOT a Bearer header.
 */
@Slf4j
@Component
public class GeminiClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final AiProperties props;

    public GeminiClient(
            HttpClient aiHttpClient,
            @Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper,
            AiProperties props
    ) {
        this.httpClient = aiHttpClient;
        this.mapper = aiObjectMapper;
        this.props = props;
    }

    /**
     * Perform a single-turn generation call. Returns the raw model
     * content string (the caller is responsible for any JSON parsing
     * of that content — we don't second-guess what format the prompt
     * asked for).
     *
     * @param systemPrompt  goes into Gemini's {@code systemInstruction} block
     * @param userPrompt    becomes a single user-role {@code contents[0]} part
     * @param jsonMode      whether to request {@code responseMimeType=application/json}
     * @param userIdHash    short opaque identifier for cost-logging only;
     *                      should NOT be the user's email or real id —
     *                      use {@code LLMContextBuilder.pseudoIdForLogs(userId)}
     * @return  parsed result containing the model's content + token usage
     * @throws AiCallException  wraps any IO / parse failure with a friendly message
     */
    public GeminiResult chat(
            String systemPrompt,
            String userPrompt,
            boolean jsonMode,
            String userIdHash
    ) {
        if (!props.gemini().isConfigured()) {
            throw new AiCallException("Gemini API key is not configured");
        }

        GenerateRequest req = new GenerateRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userPrompt)))),
                new GenerationConfig(
                        props.gemini().temperature(),
                        props.gemini().maxTokens(),
                        jsonMode ? "application/json" : null,
                        // Gemini 2.5 / flash-latest spend hidden "thinking"
                        // tokens out of the maxOutputTokens budget. For
                        // structured ranking over a pre-filtered job pool
                        // we don't need extended reasoning — disable it so
                        // the full budget goes to the JSON we actually
                        // want. Saves cost AND avoids truncated responses.
                        new ThinkingConfig(0)
                )
        );

        String body;
        try {
            body = mapper.writeValueAsString(req);
        } catch (Exception e) {
            throw new AiCallException("Failed to serialize generate request", e);
        }

        String url = props.gemini().baseUrl()
                + "/v1beta/models/" + props.gemini().model()
                + ":generateContent";

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(props.gemini().timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-goog-api-key", props.gemini().apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long startNanos = System.nanoTime();
        String requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        HttpResponse<String> response;
        try {
            response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiCallException("Gemini call interrupted");
        } catch (Exception e) {
            log.warn("[ai] {} network error after {}ms: {}",
                    requestId, elapsedMs(startNanos), e.getMessage());
            throw new AiCallException("Could not reach the AI provider", e);
        }
        long elapsedMs = elapsedMs(startNanos);

        if (response.statusCode() >= 400) {
            // Truncate the body so a verbose 4xx doesn't blow up the log
            // aggregator. Body never contains the request prompt — just
            // provider-side error messaging.
            String snippet = response.body() == null ? ""
                    : response.body().substring(0, Math.min(300, response.body().length()));
            log.warn("[ai] {} Gemini HTTP {} in {}ms body={}",
                    requestId, response.statusCode(), elapsedMs, snippet);
            throw new AiCallException("AI provider returned HTTP " + response.statusCode());
        }

        GenerateResponse parsed;
        try {
            parsed = mapper.readValue(response.body(), GenerateResponse.class);
        } catch (Exception e) {
            // Include the exception message + a truncated body snippet so
            // schema drift from Google is diagnosable from logs alone.
            String snippet = response.body() == null ? ""
                    : response.body().substring(0, Math.min(400, response.body().length()));
            log.warn("[ai] {} could not parse Gemini response in {}ms: {} — body snippet: {}",
                    requestId, elapsedMs, e.getMessage(), snippet);
            throw new AiCallException("Malformed AI response", e);
        }
        if (parsed.candidates() == null || parsed.candidates().isEmpty()
                || parsed.candidates().get(0).content() == null
                || parsed.candidates().get(0).content().parts() == null
                || parsed.candidates().get(0).content().parts().isEmpty()) {
            throw new AiCallException("AI provider returned no candidates");
        }

        // Gemini may split a single response into multiple text parts; concatenate.
        StringBuilder content = new StringBuilder();
        for (Part p : parsed.candidates().get(0).content().parts()) {
            if (p.text() != null) content.append(p.text());
        }

        // Make truncation visible in the logs — a finishReason of MAX_TOKENS
        // means the body is incomplete JSON and downstream parsing will fail.
        String finishReason = parsed.candidates().get(0).finishReason();
        if ("MAX_TOKENS".equals(finishReason) || "OTHER".equals(finishReason)) {
            log.warn("[ai] {} response cut off (finishReason={}). Consider raising app.ai.gemini.max-tokens.",
                    requestId, finishReason);
        }

        int tokensIn = parsed.usageMetadata() == null ? 0 : parsed.usageMetadata().promptTokenCount();
        int tokensOut = parsed.usageMetadata() == null ? 0 : parsed.usageMetadata().candidatesTokenCount();

        // Cost log — model, tokens, latency, opaque user id. NO prompt, NO content.
        log.info("[ai-cost] {} model={} user={} tokens_in={} tokens_out={} latency_ms={}",
                requestId,
                props.gemini().model(),
                userIdHash,
                tokensIn,
                tokensOut,
                elapsedMs
        );

        return new GeminiResult(content.toString(), tokensIn, tokensOut, elapsedMs);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /* ---------------------------------------------------------------- *
     *  Wire-format records — match Gemini's generateContent schema.    *
     *  See https://ai.google.dev/api/generate-content                  *
     * ---------------------------------------------------------------- */

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerateRequest(
            SystemInstruction systemInstruction,
            List<Content> contents,
            GenerationConfig generationConfig
    ) {}

    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(
            Double temperature,
            Integer maxOutputTokens,
            String responseMimeType,
            ThinkingConfig thinkingConfig
    ) {}

    /**
     * Controls the hidden chain-of-thought budget on Gemini 2.5 / flash-latest
     * models. {@code thinkingBudget = 0} disables thinking entirely. Older
     * (pre-2.5) models silently ignore this — safe to always send.
     */
    public record ThinkingConfig(int thinkingBudget) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerateResponse(
            List<Candidate> candidates,
            UsageMetadata usageMetadata
    ) {}

    public record Candidate(
            Content content,
            String finishReason,
            Integer index
    ) {}

    public record UsageMetadata(
            int promptTokenCount,
            int candidatesTokenCount,
            int totalTokenCount
    ) {}

    /** Caller-facing result. {@code rawContent} is whatever the model returned. */
    public record GeminiResult(
            String rawContent,
            int tokensIn,
            int tokensOut,
            long latencyMs
    ) {}

    /** Generic upstream-failure exception caught by the service layer. */
    public static class AiCallException extends RuntimeException {
        public AiCallException(String message) { super(message); }
        public AiCallException(String message, Throwable cause) { super(message, cause); }

        // Helps keep JsonNode imports tidy even if we want to inspect parsed bodies later.
        @SuppressWarnings("unused")
        public JsonNode unused() { return null; }
    }
}
