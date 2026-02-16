package com.hostdesign24.jobportal.mapper;

import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.model.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class FileMapper {

  @Mapping(target = "url", expression = "java(buildFullUrl(file.getUrl(), publicUrl))")
  public abstract FileDto toDto(File file, String publicUrl);

  public abstract File toEntity(FileDto dto);

  protected String buildFullUrl(String url, String publicUrl) {
    if (url == null) {
      return null;
    }
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }
    return publicUrl + url;
  }
}

