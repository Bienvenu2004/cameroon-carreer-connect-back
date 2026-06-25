package com.hostdesign24.jobportal.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Wire body for {@code POST /api/hjp/ai/search} — the natural-language
 * job search endpoint backing the §5.2 AI Semantic Job Search feature.
 *
 * Kept tiny on purpose: we only ever pass the user's raw query string.
 * The LLM does the rest. {@code size} is optional and clamped by the
 * service layer to a safe upper bound so a malicious client can't ask
 * for a million results.
 */
@Getter
@Setter
@NoArgsConstructor
public class AiSearchRequestDto {

    /** The user's free-form query in EN or FR. 1..500 chars. */
    @NotBlank
    @Size(max = 500, message = "Query too long (max 500 chars)")
    private String query;

    /** Desired page size; defaults to 12 server-side. Clamped to [1, 50]. */
    private Integer size;
}
