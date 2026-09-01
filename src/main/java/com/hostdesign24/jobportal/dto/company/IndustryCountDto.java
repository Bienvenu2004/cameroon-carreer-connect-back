package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.model.enums.Industry;

/**
 * How many approved companies sit in one industry.
 *
 * Powers the "browse by industry" directory. Counting on the server is the whole
 * point: the alternative is twenty separate list calls from the browser to
 * discover twenty numbers, which is exactly the kind of thing that makes a page
 * unusable on a metered connection.
 *
 * @param industry the industry
 * @param count    approved, non-deleted companies in it
 */
public record IndustryCountDto(Industry industry, long count) {
}
