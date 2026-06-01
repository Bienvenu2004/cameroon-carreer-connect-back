package com.hostdesign24.jobportal.ai.service;

import com.hostdesign24.jobportal.ai.dto.AnonymizedProfileContext;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.Skill;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * The ONLY path from a {@code JobSeekerProfile} entity to anything an LLM
 * call ever sees. Strips identifying data and returns a flat
 * {@link AnonymizedProfileContext}.
 *
 * Audit-grade. Don't bypass this. If you find yourself reaching for a
 * profile field that isn't exposed here, the right answer is to think
 * carefully about whether it's safe to send to the provider, then add it
 * explicitly to this builder + AnonymizedProfileContext — not to call
 * the LLM with a different shape.
 */
@Component
public class LLMContextBuilder {

    /**
     * Build a safe profile context for prompts. Tolerates partial profiles —
     * empty / null fields are passed through as null/empty collections.
     */
    public AnonymizedProfileContext fromProfile(JobSeekerProfile profile) {
        if (profile == null) {
            // Empty context — service layer will short-circuit before
            // calling the LLM with this.
            return new AnonymizedProfileContext(
                    "Candidate", List.of(), List.of(),
                    null, null, null, null,
                    null, null, null, null, null
            );
        }

        List<String> skills = profile.getSkills() == null ? List.of()
                : profile.getSkills().stream()
                    .map(Skill::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .distinct()
                    .toList();

        List<String> langs = profile.getSpokenLanguages() == null ? List.of()
                : Arrays.stream(profile.getSpokenLanguages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();

        return new AnonymizedProfileContext(
                "Candidate",
                skills,
                langs,
                deriveYearsOfExperience(profile),       // null until we ship WorkExperience
                profile.getEmploymentType(),
                profile.getWorkAuthorization(),
                profile.getAddress() == null ? null : profile.getAddress().getRegion(),
                null,                                   // salary band — comes from JobPreferences (future)
                null,
                null,
                null,                                   // preferredLanguage — comes from JobPreferences (future)
                deriveTargetRoleHint(profile)
        );
    }

    /**
     * Years of experience is a key matching signal but we don't yet have
     * a {@code WorkExperience} entity. Returning null here is honest —
     * the prompt template will mention "experience unknown" rather than
     * fabricate a number. Wire this up when WorkExperience lands.
     */
    private Integer deriveYearsOfExperience(JobSeekerProfile profile) {
        return null;
    }

    /**
     * "What kind of role does this person want?" — a free-text hint the
     * LLM can use to bias ranking. Until we have a target-roles array on
     * a JobPreferences entity, return null and let the model rely purely
     * on skills + languages.
     */
    private String deriveTargetRoleHint(JobSeekerProfile profile) {
        return null;
    }

    /**
     * Short, deterministic, opaque identifier safe to log alongside
     * cost / latency metrics. NOT cryptographically reversible by an
     * attacker who doesn't have the raw user id (SHA-256, truncated).
     *
     * Use this — not the user UUID — anywhere logs or telemetry leave
     * the application boundary.
     */
    public String pseudoIdForLogs(UUID userId) {
        if (userId == null) return "anon";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(userId.toString().getBytes());
            return HexFormat.of().formatHex(digest).substring(0, 10);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JDK; this branch is unreachable.
            return "anon";
        }
    }
}
