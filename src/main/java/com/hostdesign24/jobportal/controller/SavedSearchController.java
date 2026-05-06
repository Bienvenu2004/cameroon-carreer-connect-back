package com.hostdesign24.jobportal.controller;

import com.hostdesign24.jobportal.dto.common.ApiResponse;
import com.hostdesign24.jobportal.dto.savedsearch.SavedSearchDto;
import com.hostdesign24.jobportal.services.SavedSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hjp/saved-searches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('JOB_SEEKER')")
public class SavedSearchController {

    private final SavedSearchService service;

    @GetMapping
    public ApiResponse<List<SavedSearchDto>> listMine() {
        return ApiResponse.success(service.listMine(), "Saved searches retrieved");
    }

    @PostMapping
    public ApiResponse<SavedSearchDto> create(@Valid @RequestBody SavedSearchDto dto) {
        return ApiResponse.success(service.create(dto), "Saved search created");
    }

    @PatchMapping("/{id}")
    public ApiResponse<SavedSearchDto> update(@PathVariable UUID id, @Valid @RequestBody SavedSearchDto dto) {
        return ApiResponse.success(service.update(id, dto), "Saved search updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.success("Saved search deleted");
    }
}
