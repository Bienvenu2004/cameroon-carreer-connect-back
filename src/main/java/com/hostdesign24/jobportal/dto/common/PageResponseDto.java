package com.hostdesign24.jobportal.dto.common;

import java.util.List;

public record PageResponseDto<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isLast
) {}
