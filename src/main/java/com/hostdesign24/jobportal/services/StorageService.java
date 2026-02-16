package com.hostdesign24.jobportal.services;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.UUID;

public interface StorageService {
    String getLogoUrl();

    ResponseEntity<Resource> streamFile(UUID fileId, HttpHeaders headers) throws IOException;

    ResponseEntity<Resource> downloadFile(UUID fileId) throws IOException;

    @NonNull ResponseEntity<Resource> getFileFromUrl(HttpServletRequest request) throws IOException;

    ResponseEntity<Resource> getLogoFromUrl(HttpServletRequest request) throws IOException;
}
