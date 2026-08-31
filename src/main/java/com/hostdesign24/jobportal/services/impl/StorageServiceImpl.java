package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.repository.FileRepository;
import com.hostdesign24.jobportal.services.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Streams stored files back to the browser.
 *
 * Why we proxy the bytes instead of redirecting to Cloudinary:
 * PDFs are stored as Cloudinary "raw" resources whose delivery URL has no
 * `.pdf` extension, so Cloudinary serves them as `application/octet-stream`.
 * A browser can't preview that inline and, on download, saves an
 * extension-less "unknown" file. By fetching the bytes here and re-sending
 * them with the correct `Content-Type` (from the stored file metadata) and a
 * proper filename, both inline preview and download work — for files already
 * uploaded as well as new ones.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final FileRepository fileRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${app.logo-url:https://res.cloudinary.com/dz0jw1sxo/image/upload/v1/jobportal/logo/dark.png}")
    private String logoUrl;

    @Override
    public String getLogoUrl() {
        return logoUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getFileByUrl(UUID fileId) {
        return stream(fileId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(UUID fileId) {
        return stream(fileId, true);
    }

    /**
     * Fetch the file's bytes from storage and return them with headers that
     * make the browser either preview (inline) or download (attachment) it.
     */
    private ResponseEntity<Resource> stream(UUID fileId, boolean asAttachment) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        byte[] bytes;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(file.getUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.error("Storage fetch for file {} returned status {}", fileId, response.statusCode());
                return ResponseEntity.status(response.statusCode()).build();
            }
            bytes = response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted fetching file {} from storage", fileId, e);
            return ResponseEntity.status(502).build();
        } catch (Exception e) {
            log.error("Failed to fetch file {} from storage: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(502).build();
        }

        MediaType contentType;
        try {
            contentType = file.getType() != null
                    ? MediaType.parseMediaType(file.getType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename = safeFilename(file);
        ContentDisposition disposition = (asAttachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    /**
     * A download-safe filename: fall back to the file id and, for PDFs, make
     * sure the name carries a `.pdf` extension so the OS opens it correctly.
     */
    private String safeFilename(File file) {
        String name = file.getName() != null && !file.getName().isBlank()
                ? file.getName()
                : file.getId().toString();
        if ("application/pdf".equalsIgnoreCase(file.getType())
                && !name.toLowerCase().endsWith(".pdf")) {
            name = name + ".pdf";
        }
        // Strip characters that would break the Content-Disposition header.
        return name.replaceAll("[\\r\\n\"]", "_");
    }
}
