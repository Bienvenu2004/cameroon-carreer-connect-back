package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.FileRepository;
import com.hostdesign24.jobportal.services.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final FileRepository fileRepository;

    @Value("${app.storage.base-dir}")
    private String storageBaseDir;

    @Value("${app.logo-url}")
    private String logoUrl;

    @Value("${app.storage.logo-dir}")
    private String logoDir;

    @Override
    public String getLogoUrl() {
        return logoUrl;
    }

    @Override
    public ResponseEntity<Resource> streamFile(UUID fileId, HttpHeaders headers) throws IOException {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));


        String storedFilename = file.getUrl()
                .substring(file.getUrl().lastIndexOf("/") + 1);

        String userId = file.getCreatedBy().toString();

        Path filePath = Paths.get(storageBaseDir, userId)
                .resolve(storedFilename)
                .normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = Files.size(filePath);
        String contentType = file.getType();
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        List<HttpRange> ranges = headers.getRange();

        if (ranges.isEmpty()) {
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(fileSize)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        HttpRange range = ranges.getFirst();
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);
        long rangeLength = end - start + 1;

        RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
        raf.seek(start);

        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                return raf.read();
            }

            @Override
            public void close() throws IOException {
                raf.close();
            }
        };

        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(rangeLength)
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileSize)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    @Override
    public ResponseEntity<Resource> downloadFile(UUID fileId) throws IOException {
        User currentUser = Utils.getCurrentUser().orElseThrow(
                () -> new RuntimeException("Unauthorized")
        );

        // Fetch file from database
        File file = fileRepository.findById(fileId).orElseThrow(
                () -> new RuntimeException("File not found")
        );

        // Verify ownership
        if (!file.getCreatedBy().equals(currentUser.getId())) {
            throw new SecurityException("Unauthorized access to file");
        }

        // Extract stored filename from URL
        String storedFilename = file.getUrl().substring(file.getUrl().lastIndexOf("/") + 1);

        Path userDir = Paths.get(storageBaseDir, currentUser.getId().toString());
        Path filePath = userDir.resolve(storedFilename).normalize();

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Determine filename presented to the client
        String originalFilename = file.getName();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = filePath.getFileName().toString();
        }

        // Encode filename for Content-Disposition header
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)
                .replace("\\+", "%20");

        String contentType = file.getType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = Files.probeContentType(filePath);
        }
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + originalFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getSize()))
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }

    @Override
    public @NonNull ResponseEntity<Resource> getFileFromUrl(HttpServletRequest request) throws IOException {
        String relativePath = request
                .getRequestURI()
                .replace("/storage/", "");

        String finalPath = storageBaseDir;
        Path filePath = Paths.get(finalPath)
                .resolve(relativePath)
                .normalize();

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        Files.probeContentType(filePath)))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .body(resource);
    }

    @Override
    public ResponseEntity<Resource> getLogoFromUrl(HttpServletRequest request) throws IOException {
        String relativePath = request
                .getRequestURI()
                .replace("/logo/", "");

        String finalPath = logoDir;
        Path filePath = Paths.get(finalPath)
                .resolve(relativePath)
                .normalize();

        log.info("Logo file path: " + filePath);

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        Files.probeContentType(filePath)))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
                .body(resource);
    }
}
