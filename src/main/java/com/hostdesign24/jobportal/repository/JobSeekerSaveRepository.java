package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.JobSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobSeekerSaveRepository extends JpaRepository<JobSave, UUID>,
        JpaSpecificationExecutor<JobSave> {

    /**
     * All saved-job rows linking a given seeker profile to a given job.
     * Almost always 0 or 1 row in practice, but we return a List so we
     * can clean up any historical duplicates created by the previous
     * non-toggling implementation in a single shot.
     */
    List<JobSave> findByJobIdAndProfileIdAndDeletedFalse(UUID jobId, UUID profileId);
}
