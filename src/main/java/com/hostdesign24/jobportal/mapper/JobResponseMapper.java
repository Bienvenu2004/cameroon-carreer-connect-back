package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.JobPostResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyResponseDto;
import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Job → JobPostResponseDto mapper. Field names now match between the
 * source entity and the target DTO (id / title / location / company),
 * so most mappings happen automatically. We still declare explicit
 * sub-mappings for the nested Address → JobLocationDto and the
 * Company → JobCompanyResponseDto so MapStruct knows which fields
 * to populate from the richer entity types.
 *
 * Note: company.logo is enriched separately in JobServiceImpl.buildResponse()
 * to inject the public storage URL — we ignore it here.
 */
@Mapper(componentModel = "spring")
public interface JobResponseMapper {

    @Mapping(target = "totalCandidates", ignore = true)
    JobPostResponseDto toResponse(Job job);

    JobLocationDto addressToLocation(Address address);

    @Mapping(target = "logo", ignore = true)
    JobCompanyResponseDto companyToDto(Company company);
}

