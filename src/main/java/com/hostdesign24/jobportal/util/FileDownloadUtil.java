package com.hostdesign24.jobportal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class FileDownloadUtil {

    public Resource getFileAsResource(String downloadDir, String fileName) throws IOException {
        Path path = Paths.get(downloadDir);
        
        try (Stream<Path> list = Files.list(path)) {
            Path foundFile = list
                    .filter(file -> file.getFileName().toString().startsWith(fileName))
                    .findFirst()
                    .orElse(null);

            if (foundFile != null) {
                return new UrlResource(foundFile.toUri());
            }
        }
        
        return null;
    }
}
