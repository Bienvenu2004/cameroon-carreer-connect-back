package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.company.CompanyAddressDto;
import com.hostdesign24.jobportal.dto.company.CompanyEntryDto;
import com.hostdesign24.jobportal.dto.company.CompanyPatchDto;
import com.hostdesign24.jobportal.dto.company.CompanyResponseDto;
import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.Company;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    @Mapping(target = "logo", ignore = true)
    Company toEntity(CompanyEntryDto dto);

    CompanyResponseDto toResponse(Company company);

    CompanyAddressDto toAddressDto(Address address);

    Address toAddress(CompanyAddressDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateFromPatchDto(CompanyPatchDto dto, @MappingTarget Company company);
}

