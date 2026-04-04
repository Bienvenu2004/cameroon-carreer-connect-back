package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CompanyFilterDto extends FilterDto {

    private String name;

    private String city;

    private String stateRegion;

    private String country;

    private UUID logoId;
}

