package com.hostdesign24.jobportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.storage.base-dir}")
    private String storageBaseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path p = Paths.get(storageBaseDir).toAbsolutePath().normalize();
        String fsLocation = p.toUri().toString(); // yields file:/...
        registry.addResourceHandler("/files/**")
                .addResourceLocations(fsLocation)
                .setCachePeriod(3600);
    }
}
