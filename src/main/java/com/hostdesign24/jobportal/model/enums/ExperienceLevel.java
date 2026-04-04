package com.hostdesign24.jobportal.model.enums;

public enum ExperienceLevel {

    INTERNSHIP(0, 0),
    ENTRY_LEVEL(0, 2),
    JUNIOR(1, 3),
    MID_LEVEL(3, 5),
    SENIOR(5, 10),
    LEAD(7, 15),
    MANAGER(8, 20),
    DIRECTOR(10, 30);

    private final int minYears;
    private final int maxYears;

    ExperienceLevel(int minYears, int maxYears) {
        this.minYears = minYears;
        this.maxYears = maxYears;
    }

    public boolean matches(int years) {
        return years >= minYears && years <= maxYears;
    }

    public static ExperienceLevel fromYears(int years) {
        for (ExperienceLevel level : ExperienceLevel.values()) {
            if (level.matches(years)) {
                return level;
            }
        }
        throw new IllegalArgumentException("No matching experience level for years: " + years);
    }
}