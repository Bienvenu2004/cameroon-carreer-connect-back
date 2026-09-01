package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.services.StorageService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/storage")
@AllArgsConstructor
public class StorageController {

    private StorageService storageService;

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> serve(@PathVariable UUID fileId) {
        return storageService.getFileByUrl(fileId);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable UUID fileId) {
        return storageService.downloadFile(fileId);
    }
}
