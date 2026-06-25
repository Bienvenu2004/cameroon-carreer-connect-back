package com.hostdesign24.jobportal.services;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface StorageService {
    String getLogoUrl();

    ResponseEntity<Void> getFileByUrl(UUID fileId);

    ResponseEntity<Void> downloadFile(UUID fileId);
}
