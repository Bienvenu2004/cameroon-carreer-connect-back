package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.dto.file.FileDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobCompanyResponseDto {
    private String name;
    private FileDto logo;
}
