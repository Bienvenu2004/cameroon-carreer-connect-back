package com.hostdesign24.jobportal.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.dto.common.PageResponseDto;
import com.hostdesign24.jobportal.dto.file.FileDto;
import com.hostdesign24.jobportal.dto.file.FileFilterDto;
import com.hostdesign24.jobportal.dto.file.FileStatsDto;
import com.hostdesign24.jobportal.exception.FileUploadException;
import com.hostdesign24.jobportal.mapper.FileMapper;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.repository.FileRepository;
import com.hostdesign24.jobportal.repository.specifications.FileSpecification;
import com.hostdesign24.jobportal.services.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private static final List<String> ALLOWED_FILES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/x-ms-wmv", "video/mpeg"
    );

    private final FileRepository fileRepository;
    private final FileSpecification fileSpecification;
    private final FileMapper fileMapper;
    private final Cloudinary cloudinary;

    @Value("${spring.servlet.multipart.max-file-size:50MB}")
    private DataSize maxFileSize;

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        File file = fileRepository.findById(fileId).orElseThrow(
                () -> new FileUploadException("File not found")
        );

        fileRepository.delete(file);

        if (file.getPublicId() != null) {
            try {
                String resourceType = resolveResourceType(file.getType());
                cloudinary.uploader().destroy(file.getPublicId(),
                        ObjectUtils.asMap("resource_type", resourceType));
            } catch (Exception e) {
                log.error("Failed to delete file from Cloudinary '{}': {}", file.getPublicId(), e.getMessage(), e);
                throw new FileUploadException("Error while deleting file from storage");
            }
        }
    }

    @Override
    public File findById(UUID fileId) {
        return fileRepository.findById(fileId).orElseThrow(
                () -> new FileUploadException("File not found")
        );
    }

    @Override
    public PageResponseDto<FileDto> getFiles(FileFilterDto filterDto) {
        Pageable pageable = filterDto.toPageable();
        Specification<File> spec = fileSpecification.build(filterDto);

        Page<File> requestPage = fileRepository.findAll(spec, pageable);

        List<FileDto> fileDtos = new ArrayList<>();
        requestPage.getContent().forEach(file -> {
            FileDto dto = fileMapper.toDto(file);
            fileDtos.add(dto);
        });

        return new PageResponseDto<>(
                fileDtos,
                requestPage.getNumber(),
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.isLast()
        );
    }

    private UUID getCurrentUserId() {
        return Utils.getCurrentUser()
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"))
                .getId();
    }

    private long safeSize(File f) {
        return f.getSize() != null ? f.getSize() : 0L;
    }

    private String safeType(File f) {
        return f.getType() != null ? f.getType() : "";
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public FileStatsDto getFileStats() {
        long allowedBytes = 512L * 1024 * 1024;

        UUID userId = getCurrentUserId();
        List<File> files = fileRepository.findByCreatedBy(userId);

        long totalSize = files.stream().mapToLong(this::safeSize).sum();
        int total = files.size();

        long imagesSize = files.stream()
                .filter(f -> safeType(f).startsWith("image/"))
                .mapToLong(this::safeSize).sum();

        int imagesCount = (int) files.stream()
                .filter(f -> safeType(f).startsWith("image/"))
                .count();

        long docsSize = files.stream()
                .filter(f -> safeType(f).equals("application/pdf"))
                .mapToLong(this::safeSize).sum();

        int docsCount = (int) files.stream()
                .filter(f -> safeType(f).equals("application/pdf"))
                .count();

        long videosSize = files.stream()
                .filter(f -> safeType(f).startsWith("video/"))
                .mapToLong(this::safeSize).sum();

        int videosCount = (int) files.stream()
                .filter(f -> safeType(f).startsWith("video/"))
                .count();

        FileStatsDto dto = new FileStatsDto();
        Map<String, FileStatsDto.Stat> map = new HashMap<>();

        map.put("all", buildStat(total, totalSize, allowedBytes));
        map.put("images", buildStat(imagesCount, imagesSize, allowedBytes));
        map.put("documents", buildStat(docsCount, docsSize, allowedBytes));
        map.put("videos", buildStat(videosCount, videosSize, allowedBytes));

        dto.setStats(map);
        return dto;
    }

    private FileStatsDto.Stat buildStat(int total, long size, long allowed) {
        FileStatsDto.Stat stat = new FileStatsDto.Stat();
        stat.setTotal(total);
        stat.setTotalSize(formatSize(size));
        stat.setUsed(formatSize(size));
        stat.setAllowed(formatSize(allowed));
        stat.setPercent(allowed == 0 ? "0%" : (size * 100 / allowed) + "%");
        return stat;
    }

    private String formatSize(long size) {
        if (size >= 1024 * 1024) return String.format("%.2f MB", size / 1024.0 / 1024.0);
        if (size >= 1024) return String.format("%.2f KB", size / 1024.0);
        return size + " B";
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("No file provided for upload");
        }

        long maxBytes = maxFileSize.toBytes();
        if (file.getSize() > maxBytes) {
            throw new FileUploadException("File size exceeds maximum allowed size");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILES.contains(contentType.toLowerCase())) {
            throw new FileUploadException("File type is not allowed");
        }
    }

    @Override
    public File uploadFile(MultipartFile file, UUID ownerId, String ownerType, String relatedEntity) {
        validateUpload(file);

        try {
            String contentType = file.getContentType();
            String resourceType = resolveResourceType(contentType);
            String folder = "jobportal/" + ownerType.toLowerCase();

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", resourceType,
                            "folder", folder
                    ));

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            long bytes = ((Number) uploadResult.get("bytes")).longValue();

            String original = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();

            File fileEntity = new File();
            fileEntity.setName(original);
            fileEntity.setUrl(url);
            fileEntity.setPublicId(publicId);
            fileEntity.setType(contentType);
            fileEntity.setOwnerId(ownerId);
            fileEntity.setOwnerType(ownerType);
            fileEntity.setSize(bytes);
            fileEntity.setRelatedEntity(relatedEntity);

            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            log.error("IO error while uploading file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadException("Error while uploading file: " + e.getMessage());
        }
    }

    private String resolveResourceType(String contentType) {
        if (contentType == null) return "auto";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        return "raw";
    }
}
