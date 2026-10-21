package com.hostdesign24.jobportal.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The AI features stopped working for weeks because the configured model
 * answered HTTP 503 to every request and the client had nothing else to try.
 *
 * These tests run the real client against a stand-in for Google's API on
 * localhost: no key, no network, no cost. Each model name is scripted to
 * answer with a given status, and the order the client asks them in is
 * recorded.
 */
class GeminiClientFallbackTest {

    private static final String OK_BODY = """
            {"candidates":[{"content":{"parts":[{"text":"{\\"ok\\":true}"}]},"finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":12,"candidatesTokenCount":4}}
            """;

    private HttpServer server;
    private final List<String> asked = new CopyOnWriteArrayList<>();

    /** Status each fake model answers with. */
    private Map<String, Integer> statuses = Map.of();

    @BeforeEach
    void startFakeGemini() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/", exchange -> {
            // Path shape: /v1beta/models/{model}:generateContent
            String path = exchange.getRequestURI().getPath();
            String model = path.substring("/v1beta/models/".length(), path.indexOf(':'));
            asked.add(model);

            int status = statuses.getOrDefault(model, 404);
            byte[] body = (status == 200 ? OK_BODY : "{\"error\":{\"code\":" + status + "}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopFakeGemini() {
        server.stop(0);
    }

    @Test
    @DisplayName("an overloaded model falls through to the next one, skipping a retired name")
    void fallsBackPastUnavailableModels() {
        statuses = Map.of("busy", 503, "retired", 404, "lite", 200);

        GeminiClient.GeminiResult result = client("busy", "retired", "lite")
                .chat("system", "user", true, "test");

        assertEquals(List.of("busy", "retired", "lite"), asked, "models must be tried in order");
        assertEquals("lite", result.model(), "the result must name the model that answered");
        assertTrue(result.rawContent().contains("ok"));
    }

    @Test
    @DisplayName("a rate-limited model also falls through")
    void fallsBackOnRateLimit() {
        statuses = Map.of("limited", 429, "lite", 200);

        assertEquals("lite", client("limited", "lite").chat("s", "u", true, "t").model());
    }

    @Test
    @DisplayName("a rejected key stops at once instead of trying every model")
    void doesNotFallBackOnAuthFailure() {
        statuses = Map.of("primary", 403, "lite", 200);

        assertThrows(GeminiClient.AiCallException.class, () -> client("primary", "lite").chat("s", "u", true, "t"));
        assertEquals(List.of("primary"), asked, "a 403 would fail on every model, so none other is asked");
    }

    @Test
    @DisplayName("when every model is unavailable the caller gets an AiCallException")
    void failsWhenEveryModelIsUnavailable() {
        statuses = Map.of("busy", 503, "also-busy", 503);

        assertThrows(GeminiClient.AiCallException.class, () -> client("busy", "also-busy").chat("s", "u", true, "t"));
        assertEquals(List.of("busy", "also-busy"), asked);
    }

    private GeminiClient client(String model, String... fallbacks) {
        AiProperties props = new AiProperties(
                new AiProperties.Gemini(
                        "http://127.0.0.1:" + server.getAddress().getPort(),
                        "test-key",
                        model,
                        0.3,
                        1024,
                        5,
                        -1,
                        List.of(fallbacks)),
                null);
        return new GeminiClient(HttpClient.newHttpClient(), new ObjectMapper(), props);
    }
}
