package com.hostdesign24.jobportal.dto.common;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BaseResponseDto {

  private UUID id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID createdBy;
}
