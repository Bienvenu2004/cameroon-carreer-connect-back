package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.common.utils.Utils;
import com.hostdesign24.jobportal.model.File;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.repository.FileRepository;
import com.hostdesign24.jobportal.services.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final FileRepository fileRepository;

    @Value("${app.logo-url:https://res.cloudinary.com/dz0jw1sxo/image/upload/v1/jobportal/logo/dark.png}")
    private String logoUrl;

    @Override
    public String getLogoUrl() {
        return logoUrl;
    }

    @Override
    public ResponseEntity<Void> getFileByUrl(UUID fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(file.getUrl()))
                .build();
    }

    @Override
    public ResponseEntity<Void> downloadFile(UUID fileId) {
        User currentUser = Utils.getCurrentUser().orElseThrow(
                () -> new RuntimeException("Unauthorized")
        );

        File file = fileRepository.findById(fileId).orElseThrow(
                () -> new RuntimeException("File not found")
        );

        if (!file.getCreatedBy().equals(currentUser.getId())) {
            throw new SecurityException("Unauthorized access to file");
        }

        String downloadUrl = file.getUrl();
        if (file.getType() != null && file.getType().equals("application/pdf")) {
            downloadUrl = downloadUrl.contains("?")
                    ? downloadUrl + "&fl_attachment=true"
                    : downloadUrl + "?fl_attachment=true";
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(downloadUrl))
                .build();
    }
}
