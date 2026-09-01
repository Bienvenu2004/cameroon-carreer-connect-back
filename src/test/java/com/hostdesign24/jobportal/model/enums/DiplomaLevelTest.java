package com.hostdesign24.jobportal.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Bac+N ladder.
 *
 * This drives both candidate search ("Bac+3 minimum") and job filtering ("show
 * me what I qualify for"), so getting the ordering wrong would quietly hide
 * candidates from recruiters and jobs from seekers with no visible error.
 */
class DiplomaLevelTest {

    @Test
    @DisplayName("the ladder is ordered the way the national system is")
    void bacPlusOrdering() {
        assertThat(DiplomaLevel.BEPC.getBacPlus()).isLessThan(DiplomaLevel.BACCALAUREAT.getBacPlus());
        assertThat(DiplomaLevel.BACCALAUREAT.getBacPlus()).isLessThan(DiplomaLevel.BTS.getBacPlus());
        assertThat(DiplomaLevel.BTS.getBacPlus()).isLessThan(DiplomaLevel.LICENCE.getBacPlus());
        assertThat(DiplomaLevel.LICENCE.getBacPlus()).isLessThan(DiplomaLevel.MASTER_2.getBacPlus());
        assertThat(DiplomaLevel.MASTER_2.getBacPlus()).isLessThan(DiplomaLevel.DOCTORAT.getBacPlus());
    }

    @Test
    @DisplayName("Baccalaureat is the Bac+0 datum")
    void baccalaureatIsZero() {
        assertThat(DiplomaLevel.BACCALAUREAT.getBacPlus()).isZero();
        assertThat(DiplomaLevel.LICENCE.getBacPlus()).isEqualTo(3);
    }

    @Test
    @DisplayName("a higher qualification satisfies a lower requirement")
    void higherSatisfiesLower() {
        assertThat(DiplomaLevel.MASTER_2.satisfies(DiplomaLevel.LICENCE)).isTrue();
        assertThat(DiplomaLevel.DOCTORAT.satisfies(DiplomaLevel.BACCALAUREAT)).isTrue();
    }

    @Test
    @DisplayName("a lower qualification does not satisfy a higher requirement")
    void lowerDoesNotSatisfyHigher() {
        assertThat(DiplomaLevel.BACCALAUREAT.satisfies(DiplomaLevel.LICENCE)).isFalse();
        assertThat(DiplomaLevel.BTS.satisfies(DiplomaLevel.MASTER_2)).isFalse();
    }

    @Test
    @DisplayName("a qualification satisfies its own level")
    void satisfiesItself() {
        for (DiplomaLevel level : DiplomaLevel.values()) {
            assertThat(level.satisfies(level)).as("%s satisfies itself", level).isTrue();
        }
    }

    @Test
    @DisplayName("equal-rank qualifications are interchangeable")
    void equalRanksAreInterchangeable() {
        // BTS and DUT are both Bac+2 and adverts treat them as equivalent.
        assertThat(DiplomaLevel.BTS.satisfies(DiplomaLevel.DUT)).isTrue();
        assertThat(DiplomaLevel.DUT.satisfies(DiplomaLevel.BTS)).isTrue();
    }

    @Test
    @DisplayName("no stated requirement is satisfied by anything")
    void nullRequirementAlwaysSatisfied() {
        assertThat(DiplomaLevel.BEPC.satisfies(null)).isTrue();
    }
}
