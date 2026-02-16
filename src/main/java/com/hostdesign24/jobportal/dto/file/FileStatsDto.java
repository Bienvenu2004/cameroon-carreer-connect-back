package com.hostdesign24.jobportal.dto.file;

import lombok.Data;

import java.util.Map;

@Data
public class FileStatsDto {
    private Map<String, Stat> stats;

    @Data
    public static class Stat {
        private int total;
        private String totalSize;
        private String used;
        private String allowed;
        private String percent;
    }
}
