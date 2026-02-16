package com.hostdesign24.jobportal.services.impl;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;


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

    @Value("${app.storage.base-dir}")
    private String storageBaseDir;

    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private DataSize maxFileSize;

    @Value("${app.storage.base-url}")
    private String publicUrl;

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        File file = fileRepository.findById(fileId).orElseThrow(
                () -> new FileUploadException("File not found")
        );

        fileRepository.delete(file);


        if (file.getPublicId() != null) {
            Path path = Paths.get(storageBaseDir).resolve(file.getPublicId()).toAbsolutePath().normalize();
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                log.error("Failed to delete file from disk '{}': {}", path, e.getMessage(), e);
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

        requestPage.getContent().forEach(file ->{
            FileDto dto = fileMapper.toDto(file, publicUrl);
            fileDtos.add(dto);
        } );



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
        long allowedBytes = 512L * 1024 * 1024; // 512 MB

        UUID userId = getCurrentUserId();
        List<File> files = fileRepository.findByCreatedBy(userId);

        long totalSize = files.stream().mapToLong(this::safeSize).sum();
        int total = files.size();

        long imagesSize = files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("image/png")
                            || type.equalsIgnoreCase("image/jpg")
                            || type.equalsIgnoreCase("image/jpeg");
                })
                .mapToLong(this::safeSize).sum();

        int imagesCount = (int) files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("image/png")
                            || type.equalsIgnoreCase("image/jpg")
                            || type.equalsIgnoreCase("image/jpeg")
                            || type.equalsIgnoreCase("image/gif")
                            || type.equalsIgnoreCase("image/webp");
                }).count();

        long docsSize = files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("application/pdf")
                            || type.equalsIgnoreCase("application/docx");
                })
                .mapToLong(this::safeSize).sum();

        int docsCount = (int) files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("application/pdf")
                            || type.equalsIgnoreCase("application/docx");
                }).count();

        long videosSize = files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("video/mp4")
                            || type.equalsIgnoreCase("video/avi")
                            || type.equalsIgnoreCase("video/webm")
                            || type.equalsIgnoreCase("video/quicktime")
                            || type.equalsIgnoreCase("video/x-msvideo")
                            || type.equalsIgnoreCase("video/x-ms-wmv")
                            || type.equalsIgnoreCase("video/mpeg");
                })
                .mapToLong(this::safeSize).sum();

        int videosCount = (int) files.stream()
                .filter(f -> {
                    String type = safeType(f);
                    return type.equalsIgnoreCase("video/mp4")
                            || type.equalsIgnoreCase("video/avi")
                            || type.equalsIgnoreCase("video/webm")
                            || type.equalsIgnoreCase("video/quicktime")
                            || type.equalsIgnoreCase("video/x-msvideo")
                            || type.equalsIgnoreCase("video/x-ms-wmv")
                            || type.equalsIgnoreCase("video/mpeg");

                }).count();

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

    private String detectFileType(InputStream inputStream) throws IOException {
        byte[] headerBytes = new byte[12];

        // Wrap in BufferedInputStream if mark not supported
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        inputStream.mark(headerBytes.length);
        int bytesRead = inputStream.read(headerBytes);
        inputStream.reset();

        if (bytesRead < 4) {
            return null;
        }

        BiPredicate<Integer, int[]> matches = getIntegerBiPredicate(bytesRead, headerBytes);

        String contentType = getFileType(matches, bytesRead);
        if (contentType != null) return contentType;

        if (bytesRead >= 8 &&
                (headerBytes[4] & 0xFF) == 0x66 && // 'f'
                (headerBytes[5] & 0xFF) == 0x74 && // 't'
                (headerBytes[6] & 0xFF) == 0x79 && // 'y'
                (headerBytes[7] & 0xFF) == 0x70) { // 'p'
            // Check brand at bytes 8-11
            if (bytesRead >= 12) {
                String brand = new String(headerBytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
                if (brand.startsWith("qt")) {
                    return "video/quicktime";
                }
            }
            return "video/mp4";
        }


        if (
                (headerBytes[0] & 0xFF) == 0x30 &&
                        (headerBytes[1] & 0xFF) == 0x26 &&
                        (headerBytes[2] & 0xFF) == 0xB2 &&
                        (headerBytes[3] & 0xFF) == 0x75) {
            return "video/x-ms-wmv";
        }

        // MPEG program stream (00 00 01 BA)
        if (
                (headerBytes[0] & 0xFF) == 0x00 &&
                        (headerBytes[1] & 0xFF) == 0x00 &&
                        (headerBytes[2] & 0xFF) == 0x01 &&
                        (headerBytes[3] & 0xFF) == 0xBA) {
            return "video/mpeg";
        }

        return null;
    }

    private static String getFileType(BiPredicate<Integer, int[]> matches, int bytesRead) {
        if (matches.test(0, new int[]{0xFF, 0xD8, 0xFF})) {
            return "image/jpeg";
        }

        if (matches.test(0, new int[]{0x89, 0x50, 0x4E, 0x47})) {
            return "image/png";
        }

        if (matches.test(0, new int[]{0x47, 0x49, 0x46})) {
            return "image/gif";
        }

        if (matches.test(0, new int[]{0x25, 0x50, 0x44, 0x46})) {
            return "application/pdf";
        }

        if (bytesRead >= 12 && matches.test(0, new int[]{0x52, 0x49, 0x46, 0x46}) &&
                matches.test(8, new int[]{0x57, 0x45, 0x42, 0x50})) {
            return "image/webp";
        }

        if (matches.test(0, new int[]{0x1A, 0x45, 0xDF, 0xA3})) {
            return "video/webm";
        }

        if (
                bytesRead >= 12 &&
                        matches.test(0, new int[]{0x52, 0x49, 0x46, 0x46}) &&
                        matches.test(8, new int[]{0x41, 0x56, 0x49})) {
            return "video/x-msvideo";
        }

        return null;
    }

    private static BiPredicate<Integer, int[]> getIntegerBiPredicate(int bytesRead, byte[] headerBytes) {
        return (offset, pattern) -> {
            if (bytesRead < offset + pattern.length) return false;
            for (int i = 0; i < pattern.length; i++) {
                if ((headerBytes[offset + i] & 0xFF) != pattern[i]) return false;
            }
            return true;
        };
    }

    @Override
    public File uploadFile(MultipartFile file, UUID ownerId, String ownerType, String relatedEntity) {
        validateUpload(file);

        try (InputStream detectStream = file.getInputStream()) {
            String detected = detectFileType(detectStream);
            if (detected == null || !ALLOWED_FILES.contains(detected)) {
                throw new FileUploadException("Invalid file content type");
            }

            String original = file.getOriginalFilename() == null ? "unknown" :
                    Paths.get(file.getOriginalFilename()).getFileName().toString();

            String folderName = "";
            Path base = Paths.get(storageBaseDir).toAbsolutePath().normalize();
            Path targetDir = base.resolve(folderName);
            createTargetDirectory(targetDir);

            // Extract file extension
            String ext = "";
            int idx = original.lastIndexOf('.');
            if (idx >= 0) {
                ext = original.substring(idx);
            }

            // Generate unique filename with timestamp + UUID
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = "LandlordNde24_" + timestamp + "_" + uniqueId + ext;

            Path targetPath = targetDir.resolve(filename);

            writeFileToDisk(file, targetPath);

            String fileUrl = "storage/" + filename;

            File fileEntity = new File();
            fileEntity.setName(original);
            fileEntity.setUrl(fileUrl);
            fileEntity.setPublicId(folderName + "/" + filename);
            fileEntity.setType(detected);
            fileEntity.setOwnerId(ownerId);
            fileEntity.setOwnerType(ownerType);
            fileEntity.setSize(file.getSize());
            fileEntity.setRelatedEntity(relatedEntity);


            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            log.error("IO error while uploading file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileUploadException("Error while uploading file: " + e.getMessage());
        }
    }

    private void createTargetDirectory(Path targetDir) {
        try {
            Files.createDirectories(targetDir);
        } catch (AccessDeniedException ade) {
            log.error("Permission denied while creating directory '{}': {}", targetDir, ade.getMessage(), ade);
            // Suggest a writable fallback for local dev
            Path fallback = Paths.get(System.getProperty("user.home"), "retms", "uploads");
            log.warn("Consider using a writable directory for uploads (e.g. '{}') or create/change permissions for '{}'", fallback, targetDir);
            throw new FileUploadException("Permission denied creating storage directory " + targetDir +
                    ". Set a writable upload path (e.g. in application.properties `file.upload.dir=" + fallback + "`) " +
                    "or create/chown the directory (e.g. `sudo mkdir -p " + targetDir.getParent() + " && sudo chown -R $(whoami) " + targetDir.getParent() + "`).");
        } catch (IOException e) {
            log.error("Failed to create directory '{}': {}", targetDir, e.getMessage(), e);
            throw new FileUploadException("Could not create storage directory: " + e.getMessage());
        }
    }

    private void writeFileToDisk(MultipartFile file, Path targetPath) {
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write file to disk '{}': {}", targetPath, e.getMessage(), e);
            throw new FileUploadException("Error while saving file to disk");
        }
    }


}
