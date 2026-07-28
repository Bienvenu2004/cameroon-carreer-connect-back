package com.hostdesign24.jobportal.ai.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the {@code app.ai.*} section of application.yaml.
 *
 * Kept as a {@code record} so the values are immutable from the moment Spring
 * builds it — there's no scenario where we want to mutate the API key or
 * model name at runtime.
 *
 * The {@link Gemini#apiKey} field is intentionally allowed to be blank.
 * When it is, {@code AiRecommendationService} short-circuits and returns
 * an empty list instead of failing the request — so local dev without a
 * key still produces a usable UI (just no recommendations).
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        Gemini gemini,
        Recommendations recommendations
) {

    public record Gemini(
            String baseUrl,
            String apiKey,
            String model,
            double temperature,
            int maxTokens,
            int timeoutSeconds,
            Integer thinkingBudget
    ) {
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }

        /**
         * Whether an explicit thinking budget should be sent to Gemini.
         * Only a positive budget is valid: Gemini 2.5 also accepts 0 to
         * disable thinking, but Gemini 3.x rejects 0 with HTTP 400 — so we
         * treat null / &lt;= 0 as "omit thinkingConfig and let the model
         * decide", which is safe across all model versions.
         */
        public boolean hasThinkingBudget() {
            return thinkingBudget != null && thinkingBudget > 0;
        }
    }


}
