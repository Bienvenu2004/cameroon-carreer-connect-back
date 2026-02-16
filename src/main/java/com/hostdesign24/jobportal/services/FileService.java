package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.dto.file.FileFilterDto;
import com.hostdesign24.jobportal.dto.file.FileStatsDto;
import com.hostdesign24.jobportal.model.File;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileService {
    File uploadFile(MultipartFile file, UUID ownerId, String ownerType, String relatedEntity);

    void deleteFile(UUID fileId);

    File findById(UUID fileId);
    PageResponseDto<FileDto> getFiles(FileFilterDto filter);
    FileStatsDto getFileStats();
}
