package com.hostdesign24.jobportal.dto.file;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FileStatsDto {
    private Map<String, Stat> stats;

    @Getter
    @Setter
    public static class Stat {
        private int total;
        private String totalSize;
        private String used;
        private String allowed;
        private String percent;
    }
}
