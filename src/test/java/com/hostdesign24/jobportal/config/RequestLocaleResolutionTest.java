package com.hostdesign24.jobportal.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The AI recommendation prompt reads {@link LocaleContextHolder} to decide
 * which language to ask the model to explain itself in. That only works if the
 * {@code Accept-Language} header the browser sends actually reaches the holder
 * by the time a service runs.
 *
 * These tests pin that link against the resolver {@link I18nConfig} actually
 * declares, so a change to the supported locales or the default cannot silently
 * send every seeker back to one fixed language.
 */
class RequestLocaleResolutionTest {

    /** Reports whatever locale a service would observe mid-request. */
    @RestController
    static class LocaleProbeController {
        @GetMapping("/probe")
        String observedLocale() {
            return LocaleContextHolder.getLocale().getLanguage();
        }
    }

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new LocaleProbeController())
            .setLocaleResolver(new I18nConfig().localeResolver())
            .build();

    @Test
    @DisplayName("Accept-Language: fr is observable as French inside the request")
    void frenchHeaderReachesTheService() throws Exception {
        mvc.perform(get("/probe").header("Accept-Language", "fr"))
                .andExpect(status().isOk())
                .andExpect(content().string("fr"));
    }

    @Test
    @DisplayName("Accept-Language: en is observable as English inside the request")
    void englishHeaderReachesTheService() throws Exception {
        mvc.perform(get("/probe").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string("en"));
    }

    @Test
    @DisplayName("a regional tag resolves to a supported language")
    void regionalTagResolves() throws Exception {
        mvc.perform(get("/probe").header("Accept-Language", "en-GB,en;q=0.9"))
                .andExpect(content().string("en"));
    }

    @Test
    @DisplayName("an unsupported language and a missing header both fall back to English")
    void unsupportedAndMissingFallBackToEnglish() throws Exception {
        mvc.perform(get("/probe").header("Accept-Language", "de"))
                .andExpect(content().string("en"));
        mvc.perform(get("/probe"))
                .andExpect(content().string("en"));
    }

    @Test
    @DisplayName("the request locale does not leak into the next request")
    void localeDoesNotLeakBetweenRequests() throws Exception {
        mvc.perform(get("/probe").header("Accept-Language", "fr"))
                .andExpect(content().string("fr"));

        // A French request must not leave the holder French for whoever comes
        // next — that would hand one seeker's language to another.
        Locale afterwards = LocaleContextHolder.getLocale();
        org.assertj.core.api.Assertions.assertThat(afterwards)
                .isEqualTo(Locale.getDefault());
    }
}
