package com.hostdesign24.jobportal.model.enums;

/**
 * Academic qualification levels as they are actually named and understood in
 * Cameroon.
 *
 * The diploma is typically the first screen a Cameroonian recruiter applies —
 * ahead of skills or years worked — and job adverts are routinely written as
 * "Bac+3 minimum". A generic HIGH_SCHOOL / BACHELOR / MASTER ladder borrowed
 * from an Anglophone system would not map onto how either side of this market
 * describes itself, so the ladder below follows the national system and carries
 * the Bac+N rank that adverts are written in.
 *
 * {@code bacPlus} is the years of study past the Baccalauréat: -1 and 0 mark
 * qualifications below and at Bac level, so "at least Bac+3" is a single
 * comparison rather than a set membership test.
 */
public enum DiplomaLevel {

    /** Brevet d'Études du Premier Cycle — end of lower secondary. */
    BEPC(-1),

    /** Certificat d'Aptitude Professionnelle — vocational, lower secondary. */
    CAP(-1),

    /** Probatoire — one year before the Baccalauréat. */
    PROBATOIRE(-1),

    /** Baccalauréat / GCE Advanced Level — end of secondary. */
    BACCALAUREAT(0),

    /** Brevet de Technicien Supérieur / Higher National Diploma. */
    BTS(2),

    /** Diplôme Universitaire de Technologie. */
    DUT(2),

    /** Licence / Bachelor's degree. */
    LICENCE(3),

    /** Master 1. */
    MASTER_1(4),

    /** Master 2 / Diplôme d'Ingénieur. */
    MASTER_2(5),

    /** Doctorat / PhD. */
    DOCTORAT(8);

    private final int bacPlus;

    DiplomaLevel(int bacPlus) {
        this.bacPlus = bacPlus;
    }

    /** Years of study past the Baccalauréat; negative for sub-Bac qualifications. */
    public int getBacPlus() {
        return bacPlus;
    }

    /** True when this qualification satisfies a "Bac+N minimum" requirement. */
    public boolean satisfies(DiplomaLevel required) {
        return required == null || this.bacPlus >= required.bacPlus;
    }
}
