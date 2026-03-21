package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class FileMapper {

  @Mapping(target = "url", expression = "java(buildFullUrl(file, publicUrl))")
  public abstract FileDto toDto(File file, String publicUrl);

  public abstract File toEntity(FileDto dto);

  protected String buildFullUrl(File file, String publicUrl) {
    if (file == null || file.getUrl() == null) {
      return null;
    }
    if (file.getUrl().startsWith("http://") || file.getUrl().startsWith("https://")) {
      return file.getUrl();
    }
    return publicUrl + file.getUrl();
  }
}

