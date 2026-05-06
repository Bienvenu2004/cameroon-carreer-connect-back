package com.hostdesign24.jobportal.model.enums;

/**
 * Company headcount buckets.
 */
public enum CompanySize {
    MICRO("1-10"),
    SMALL("11-50"),
    MEDIUM("51-200"),
    LARGE("201-1000"),
    ENTERPRISE("1000+");

    private final String range;

    CompanySize(String range) {
        this.range = range;
    }

    public String getRange() {
        return range;
    }
}
