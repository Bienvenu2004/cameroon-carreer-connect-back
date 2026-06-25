package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.File;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class FileMapper {

    public abstract FileDto toDto(File file);

    public abstract File toEntity(FileDto dto);
}
