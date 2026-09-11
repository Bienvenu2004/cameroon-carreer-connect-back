package com.hostdesign24.jobportal.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The explanation attached to each recommendation is the only prose the model
 * produces that a seeker actually reads, so it has to arrive in the language
 * they are browsing in.
 *
 * The prompt used to say "reply in the same language the candidate profile
 * uses (default French)". Every label in that prompt is English, so there was
 * nothing to infer a language from and the model always took the default —
 * which is why the explanations came back in French no matter the interface
 * language. These tests pin the instruction to the resolved request locale so
 * it cannot quietly revert to a fixed language.
 */
class RecommendationPromptBuilderTest {

    private final RecommendationPromptBuilder builder = new RecommendationPromptBuilder();

    @Test
    @DisplayName("a French request asks the model for French explanations")
    void frenchLocaleAsksForFrench() {
        String prompt = builder.systemPrompt(5, Locale.FRENCH);

        assertThat(prompt).contains("Write every \"reason\" in French");
        assertThat(prompt).doesNotContain("in English, whatever language");
    }

    @Test
    @DisplayName("an English request asks the model for English explanations")
    void englishLocaleAsksForEnglish() {
        String prompt = builder.systemPrompt(5, Locale.ENGLISH);

        assertThat(prompt).contains("Write every \"reason\" in English");
        assertThat(prompt).doesNotContain("in French, whatever language");
    }

    @Test
    @DisplayName("a regional tag still resolves to its base language")
    void regionalTagsResolveToBaseLanguage() {
        assertThat(builder.systemPrompt(5, Locale.CANADA_FRENCH))
                .contains("Write every \"reason\" in French");
        assertThat(builder.systemPrompt(5, Locale.UK))
                .contains("Write every \"reason\" in English");
    }

    @Test
    @DisplayName("an unsupported or absent locale falls back to English, as I18nConfig does")
    void unsupportedLocaleFallsBackToEnglish() {
        assertThat(builder.systemPrompt(5, Locale.GERMAN))
                .contains("Write every \"reason\" in English");
        assertThat(builder.systemPrompt(5, null))
                .contains("Write every \"reason\" in English");
    }

    @Test
    @DisplayName("the JSON contract is unaffected by the language of the explanations")
    void jsonContractStaysEnglishInBothLanguages() {
        for (Locale locale : new Locale[] { Locale.FRENCH, Locale.ENGLISH }) {
            assertThat(builder.systemPrompt(5, locale))
                    .as("JSON keys for %s", locale)
                    .contains("\"recommendations\"")
                    .contains("\"jobId\"")
                    .contains("\"score\"")
                    .contains("\"reason\"")
                    .contains("Keep the JSON keys in English")
                    .contains("pick the 5 jobs that best fit");
        }
    }
}
