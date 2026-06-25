package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.savedsearch.SavedSearchDto;

import java.util.List;
import java.util.UUID;

public interface SavedSearchService {

    SavedSearchDto create(SavedSearchDto dto);

    List<SavedSearchDto> listMine();

    SavedSearchDto update(UUID id, SavedSearchDto dto);

    void delete(UUID id);

    /** Run the scheduled alert process — invoked by the scheduler. */
    void runAlerts();
}
