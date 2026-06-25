package com.hostdesign24.jobportal.model.enums;

/**
 * The 10 official administrative regions of Cameroon.
 * Used for regional categorization of jobs, companies, and addresses.
 */
public enum Region {
    ADAMAOUA("Adamaoua", "Ngaoundéré"),
    CENTRE("Centre", "Yaoundé"),
    EST("Est", "Bertoua"),
    EXTREME_NORD("Extrême-Nord", "Maroua"),
    LITTORAL("Littoral", "Douala"),
    NORD("Nord", "Garoua"),
    NORD_OUEST("Nord-Ouest", "Bamenda"),
    OUEST("Ouest", "Bafoussam"),
    SUD("Sud", "Ebolowa"),
    SUD_OUEST("Sud-Ouest", "Buea");

    private final String displayName;
    private final String capital;

    Region(String displayName, String capital) {
        this.displayName = displayName;
        this.capital = capital;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCapital() {
        return capital;
    }
}
