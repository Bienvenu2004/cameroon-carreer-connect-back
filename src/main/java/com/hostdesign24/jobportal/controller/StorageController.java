package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.services.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/storage")
@AllArgsConstructor
public class StorageController {

    private StorageService storageService;

    @GetMapping("/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) throws IOException {

        return storageService.getFileFromUrl(request);
    }

    @GetMapping("/stream/{fileId}")
    public ResponseEntity<Resource> streamFile(
            @PathVariable UUID fileId,
            @RequestHeader HttpHeaders headers
    ) throws IOException {

        return storageService.streamFile(fileId, headers);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable UUID fileId) throws IOException {

        return storageService.downloadFile(fileId);
    }

}