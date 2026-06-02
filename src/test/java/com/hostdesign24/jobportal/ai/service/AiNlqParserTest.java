package com.hostdesign24.jobportal.ai.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostdesign24.jobportal.ai.client.GeminiClient;
import com.hostdesign24.jobportal.ai.client.GeminiClient.GeminiResult;
import com.hostdesign24.jobportal.ai.dto.AiProperties;
import com.hostdesign24.jobportal.ai.dto.ParsedQuery;
import com.hostdesign24.jobportal.ai.dto.Recommendations;
import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiNlqParser}. We mock the Gemini call to return
 * canned JSON — these tests verify the JSON → ParsedQuery coercion +
 * hallucination filtering, not the model's parse quality.
 *
 * The four "spec" cases mirror the canonical examples from §5.2 of the
 * product specification verbatim. They lock in the contract: if the
 * model returns exactly those JSON shapes, our parser produces the
 * structured filter we promised the rest of the system.
 */
class AiNlqParserTest {

    private GeminiClient gemini;
    private AiNlqParser parser;

    @BeforeEach
    void setUp() {
        gemini = Mockito.mock(GeminiClient.class);
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AiProperties props = new AiProperties(
                // isConfigured() returns true when apiKey is non-blank
                new AiProperties.Gemini("https://x", "key", "model", 0.3, 1024, 30),
                new Recommendations(50, 5)
        );
        parser = new AiNlqParser(gemini, new NlqParserPromptBuilder(), props, mapper);
    }

    private void stubResponse(String json) {
        when(gemini.chat(anyString(), anyString(), anyBoolean(), eq("nlq")))
                .thenReturn(new GeminiResult(json, 100, 50, 200));
    }

    /* ----------------------------- spec examples ----------------------------- */

    @Test
    void parses_remote_junior_java_developer_query() {
        stubResponse("""
                {"keywords":["java developer","remote"],"skills":["Java"],"region":null,
                 "city":null,"jobType":null,"jobSite":"REMOTE","language":null,
                 "industry":"INFORMATION_TECHNOLOGY","level":"JUNIOR",
                 "salaryMin":null,"salaryMax":null,"confidence":0.92}
                """);
        ParsedQuery p = parser.parseQuery("Remote junior Java developer jobs in Cameroon");
        assertThat(p.jobSite()).isEqualTo(JobSite.REMOTE);
        assertThat(p.industry()).isEqualTo(Industry.INFORMATION_TECHNOLOGY);
        assertThat(p.level()).isEqualTo(ExperienceLevel.JUNIOR);
        assertThat(p.skills()).containsExactly("Java");
        assertThat(p.confidence()).isEqualTo(0.92);
        assertThat(p.isLowConfidence()).isFalse();
    }

    @Test
    void parses_marketing_internships_query() {
        stubResponse("""
                {"keywords":["marketing","internship"],"skills":[],"region":null,
                 "city":null,"jobType":"INTERN","jobSite":null,"language":null,
                 "industry":null,"level":"INTERNSHIP",
                 "salaryMin":null,"salaryMax":null,"confidence":0.90}
                """);
        ParsedQuery p = parser.parseQuery("Marketing internships for students with no experience");
        assertThat(p.jobType()).isEqualTo(JobType.INTERN);
        assertThat(p.level()).isEqualTo(ExperienceLevel.INTERNSHIP);
        assertThat(p.skills()).isEmpty();
    }

    @Test
    void parses_spring_boot_backend_query() {
        stubResponse("""
                {"keywords":["backend developer","spring boot"],
                 "skills":["Spring Boot","Java","REST APIs"],
                 "region":null,"city":null,"jobType":null,"jobSite":null,"language":null,
                 "industry":"INFORMATION_TECHNOLOGY","level":"MID_LEVEL",
                 "salaryMin":null,"salaryMax":null,"confidence":0.85}
                """);
        ParsedQuery p = parser.parseQuery("Jobs matching my Spring Boot backend experience");
        assertThat(p.skills()).containsExactly("Spring Boot", "Java", "REST APIs");
        assertThat(p.level()).isEqualTo(ExperienceLevel.MID_LEVEL);
    }

    @Test
    void parses_accounting_near_douala_query() {
        stubResponse("""
                {"keywords":["accounting","ohada"],"skills":["OHADA","Accounting"],
                 "region":"LITTORAL","city":"Douala","jobType":null,"jobSite":null,
                 "language":null,"industry":"BANKING_FINANCE","level":null,
                 "salaryMin":null,"salaryMax":null,"confidence":0.88}
                """);
        ParsedQuery p = parser.parseQuery("Accounting roles near Douala with OHADA knowledge");
        assertThat(p.region()).isEqualTo(Region.LITTORAL);
        assertThat(p.city()).isEqualTo("Douala");
        assertThat(p.industry()).isEqualTo(Industry.BANKING_FINANCE);
        assertThat(p.skills()).containsExactly("OHADA", "Accounting");
    }

    /* ----------------------------- guards & edge cases ----------------------------- */

    @Test
    void hallucinated_enum_values_are_dropped_to_null() {
        // Model returned an enum value that doesn't exist in our schema —
        // must be silently dropped, NOT propagate as a runtime crash later.
        stubResponse("""
                {"keywords":["x"],"skills":[],"region":"NORTH_POLE",
                 "jobType":"PERMANENT","jobSite":"AT_HOME","language":"SWAHILI",
                 "industry":"UFO","level":"GOD",
                 "salaryMin":null,"salaryMax":null,"confidence":0.5}
                """);
        ParsedQuery p = parser.parseQuery("weird query");
        assertThat(p.region()).isNull();
        assertThat(p.jobType()).isNull();
        assertThat(p.jobSite()).isNull();
        assertThat(p.language()).isNull();
        assertThat(p.industry()).isNull();
        assertThat(p.level()).isNull();
        assertThat(p.keywords()).containsExactly("x");  // kept what's safe
    }

    @Test
    void confidence_below_threshold_marks_low_confidence() {
        stubResponse("""
                {"keywords":["jobs"],"skills":[],"region":null,
                 "city":null,"jobType":null,"jobSite":null,"language":null,
                 "industry":null,"level":null,
                 "salaryMin":null,"salaryMax":null,"confidence":0.2}
                """);
        ParsedQuery p = parser.parseQuery("jobs");
        assertThat(p.isLowConfidence()).isTrue();
    }

    @Test
    void blank_query_returns_empty_without_calling_gemini() {
        ParsedQuery p = parser.parseQuery("   ");
        assertThat(p.confidence()).isZero();
        assertThat(p.isLowConfidence()).isTrue();
        Mockito.verifyNoInteractions(gemini);
    }

    @Test
    void empty_keywords_array_is_backfilled_with_original_query() {
        // Guarantee at least one keyword so the LIKE fallback always has
        // something to match against, even if the model returns [] here.
        stubResponse("""
                {"keywords":[],"skills":[],"region":null,
                 "city":null,"jobType":null,"jobSite":null,"language":null,
                 "industry":null,"level":null,
                 "salaryMin":null,"salaryMax":null,"confidence":0.7}
                """);
        ParsedQuery p = parser.parseQuery("data analyst");
        assertThat(p.keywords()).containsExactly("data analyst");
    }

    @Test
    void malformed_json_returns_empty_safely() {
        stubResponse("not json at all");
        ParsedQuery p = parser.parseQuery("anything");
        assertThat(p.confidence()).isZero();
        assertThat(p.keywords()).containsExactly("anything");
    }

    @Test
    void missing_gemini_key_short_circuits_to_empty() {
        AiProperties unconfigured = new AiProperties(
                new AiProperties.Gemini("https://x", "", "model", 0.3, 1024, 30),
                new Recommendations(50, 5)
        );
        ObjectMapper mapper = new ObjectMapper();
        AiNlqParser p = new AiNlqParser(gemini, new NlqParserPromptBuilder(), unconfigured, mapper);
        ParsedQuery out = p.parseQuery("Remote Java jobs");
        assertThat(out.confidence()).isZero();
        Mockito.verifyNoInteractions(gemini);
    }
}
