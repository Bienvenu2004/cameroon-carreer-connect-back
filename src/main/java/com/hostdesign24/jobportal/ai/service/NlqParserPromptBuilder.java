package com.hostdesign24.jobportal.ai.service;

import com.hostdesign24.jobportal.model.enums.ExperienceLevel;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobLanguage;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds the system + user prompts for the natural-language query parser.
 *
 * Strategy:
 *   - System prompt enumerates EVERY valid enum value so the model can't
 *     hallucinate a string like "MID" that doesn't exist in our schema.
 *     Validation at parse time catches anything that slips through, but
 *     listing them upfront drives the hallucination rate to near zero.
 *   - Four canonical few-shots come straight from the product spec §5.2.
 *     They cover the four signal axes we care most about: location,
 *     contract type, skill-driven matching, and domain-specific keywords
 *     (OHADA).
 *   - The output schema is rigid: same key set every time, null for
 *     anything the model can't infer. Keeps {@code AiNlqParser} simple.
 *
 * The prompt is bilingual-friendly by example: the few-shots are English
 * but the rules mention French equivalents (CDI/CDD, stage). Gemini
 * handles the FR↔EN mapping internally — no separate FR prompt needed.
 */
@Component
public class NlqParserPromptBuilder {

    private final String systemPrompt;

    public NlqParserPromptBuilder() {
        this.systemPrompt = buildSystemPrompt();
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userPrompt(String query) {
        // Wrap in quotes so the model never confuses the query with an
        // instruction. The prompt explicitly tells it the input arrives
        // between angle brackets.
        return "Parse this query:\n<<<" + (query == null ? "" : query.trim()) + ">>>";
    }

    /* ------------------------------------------------------------------ */

    private static String buildSystemPrompt() {
        return """
                You are a query parser for JobConnect Cameroun, a Cameroonian job-search platform.
                The user types a free-form query in French or English. Your job is to extract
                structured filter values that can be matched against our job database.

                Output STRICTLY this JSON shape — no markdown, no preamble, no explanation:
                {
                  "keywords":   ["..."],
                  "skills":     ["..."],
                  "region":     "<enum or null>",
                  "city":       "<string or null>",
                  "jobType":    "<enum or null>",
                  "jobSite":    "<enum or null>",
                  "language":   "<enum or null>",
                  "industry":   "<enum or null>",
                  "level":      "<enum or null>",
                  "salaryMin":  <integer XAF or null>,
                  "salaryMax":  <integer XAF or null>,
                  "confidence": <float 0..1>
                }

                ENUM VOCABULARIES — values must match EXACTLY (case-sensitive), or use null.
                  region:   %s
                  jobType:  %s
                  jobSite:  %s
                  language: %s
                  industry: %s
                  level:    %s

                RULES:
                - If you are uncertain about an enum value, set it to null. Never invent values.
                - Cities map to regions:
                    Yaoundé → CENTRE | Douala → LITTORAL | Bamenda → NORD_OUEST |
                    Buea → SUD_OUEST | Bafoussam → OUEST | Garoua → NORD |
                    Ngaoundéré → ADAMAOUA | Bertoua → EST | Maroua → EXTREME_NORD |
                    Ebolowa → SUD.
                  When a city is named, set BOTH "city" and "region".
                - jobSite mapping: "remote/à distance" → REMOTE; "hybrid/hybride" → HYBRID;
                  "on-site/sur site/in office" → ONSITE.
                - jobType mapping: "CDI" → FULL_TIME; "CDD" → TEMPORARY;
                  "stage/intern/internship" → INTERN; "freelance" → FREELANCE.
                - level mapping: "intern/stage" → INTERNSHIP; "no experience/débutant" →
                  ENTRY_LEVEL; "junior" → JUNIOR; "mid/intermédiaire" → MID_LEVEL;
                  "senior" → SENIOR; "lead/principal" → LEAD; "manager/chef" → MANAGER;
                  "director/directeur" → DIRECTOR.
                - "keywords" MUST contain at least one entry — use the most salient noun
                  phrases from the query, lowercased.
                - "skills" should list concrete named technologies, frameworks, or domain
                  knowledge (e.g. "Spring Boot", "OHADA", "Photoshop"). Leave empty if none.
                - Reduce "confidence" below 0.4 when the query is vague, off-topic, or you
                  could not extract any structured signal.

                EXAMPLES (input → output):

                Q: "Remote junior Java developer jobs in Cameroon"
                A: {"keywords":["java developer","remote"],"skills":["Java"],"region":null,"city":null,"jobType":null,"jobSite":"REMOTE","language":null,"industry":"INFORMATION_TECHNOLOGY","level":"JUNIOR","salaryMin":null,"salaryMax":null,"confidence":0.92}

                Q: "Marketing internships for students with no experience"
                A: {"keywords":["marketing","internship"],"skills":[],"region":null,"city":null,"jobType":"INTERN","jobSite":null,"language":null,"industry":null,"level":"INTERNSHIP","salaryMin":null,"salaryMax":null,"confidence":0.90}

                Q: "Jobs matching my Spring Boot backend experience"
                A: {"keywords":["backend developer","spring boot"],"skills":["Spring Boot","Java","REST APIs"],"region":null,"city":null,"jobType":null,"jobSite":null,"language":null,"industry":"INFORMATION_TECHNOLOGY","level":"MID_LEVEL","salaryMin":null,"salaryMax":null,"confidence":0.85}

                Q: "Accounting roles near Douala with OHADA knowledge"
                A: {"keywords":["accounting","ohada"],"skills":["OHADA","Accounting"],"region":"LITTORAL","city":"Douala","jobType":null,"jobSite":null,"language":null,"industry":"BANKING_FINANCE","level":null,"salaryMin":null,"salaryMax":null,"confidence":0.88}
                """.formatted(
                        joinEnum(Region.class),
                        joinEnum(JobType.class),
                        joinEnum(JobSite.class),
                        joinEnum(JobLanguage.class),
                        joinEnum(Industry.class),
                        joinEnum(ExperienceLevel.class)
                );
    }

    private static <E extends Enum<E>> String joinEnum(Class<E> clazz) {
        return Arrays.stream(clazz.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
