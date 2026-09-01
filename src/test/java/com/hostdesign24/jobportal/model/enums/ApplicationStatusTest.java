package com.hostdesign24.jobportal.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    @DisplayName("the three terminal states are closed")
    void terminalStatesAreClosed() {
        assertThat(ApplicationStatus.HIRED.isClosed()).isTrue();
        assertThat(ApplicationStatus.REJECTED.isClosed()).isTrue();
        assertThat(ApplicationStatus.WITHDRAWN.isClosed()).isTrue();
    }

    @Test
    @DisplayName("in-flight states are open")
    void inFlightStatesAreOpen() {
        assertThat(ApplicationStatus.APPLIED.isClosed()).isFalse();
        assertThat(ApplicationStatus.REVIEWED.isClosed()).isFalse();
        assertThat(ApplicationStatus.INTERVIEW.isClosed()).isFalse();
    }

    @Test
    @DisplayName("WITHDRAWN exists, so candidates can leave a pipeline")
    void withdrawnExists() {
        assertThat(ApplicationStatus.valueOf("WITHDRAWN")).isNotNull();
    }
}
