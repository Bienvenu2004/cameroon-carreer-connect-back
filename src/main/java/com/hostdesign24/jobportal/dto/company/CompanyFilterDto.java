package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import com.hostdesign24.jobportal.model.enums.CompanySize;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CompanyFilterDto extends FilterDto {

    private String name;

    private String city;

    private String stateRegion;

    private Region region;

    private String country;

    private Industry industry;

    private CompanySize companySize;

    private CompanyStatus status;

    private UUID logoId;
}
