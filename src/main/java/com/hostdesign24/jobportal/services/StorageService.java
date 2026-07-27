package com.hostdesign24.jobportal.services;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface StorageService {
    String getLogoUrl();

    /** Stream the file inline (for in-browser preview). */
    ResponseEntity<Resource> getFileByUrl(UUID fileId);

    /** Stream the file as an attachment (force download with a proper name). */
    ResponseEntity<Resource> downloadFile(UUID fileId);
}
