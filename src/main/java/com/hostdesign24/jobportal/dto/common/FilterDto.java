package com.hostdesign24.jobportal.dto.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class FilterDto {

  private int page = 0;
  private int size = 10;
  private String sortBy = "createdAt";
  private Sort.Direction sortOrder = Sort.Direction.DESC;

  public Pageable toPageable() {
    return PageRequest.of(page, size, Sort.by(sortOrder, sortBy));
  }
}
