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

  /**
   * Build the public URL the browser should hit to download the file.
   *
   *   - Returns null when there's nothing to serve.
   *   - Returns file.url unchanged if it's already an absolute URL.
   *   - Otherwise concatenates {@code publicUrl + file.url}, but guards
   *     against the common .env mistake where PUBLIC_URL already ends in
   *     "/storage" AND the stored url starts with "storage/" — that
   *     combination would otherwise produce ".../storagestorage/foo.pdf"
   *     which 404s in the browser even though the StorageController's
   *     `replace("/storage/", "")` happens to handle the duplicate.
   */
  protected String buildFullUrl(File file, String publicUrl) {
    if (file == null || file.getUrl() == null) {
      return null;
    }
    String stored = file.getUrl();
    if (stored.startsWith("http://") || stored.startsWith("https://")) {
      return stored;
    }
    if (publicUrl == null || publicUrl.isBlank()) {
      return stored;
    }

    // Normalize the separator between base and relative path.
    String base = publicUrl.endsWith("/")
        ? publicUrl.substring(0, publicUrl.length() - 1)
        : publicUrl;
    String path = stored.startsWith("/") ? stored.substring(1) : stored;

    // Collapse a duplicated "storage" segment when PUBLIC_URL already
    // points at the storage namespace (e.g. http://host:8080/storage)
    // and the entity's url is the historical "storage/<filename>" form.
    if (base.endsWith("/storage") && path.startsWith("storage/")) {
      path = path.substring("storage/".length());
    }

    return base + "/" + path;
  }
}

