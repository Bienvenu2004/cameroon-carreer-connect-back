package com.hostdesign24.jobportal.dto;

        import com.hostdesign24.jobportal.dto.jobActivityPost.JobCompanyDto;
        import com.hostdesign24.jobportal.dto.jobActivityPost.JobLocationDto;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        import java.util.UUID;

@Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public class RecruiterJobsDto {
            private Long totalCandidates;
            private UUID jobPostId;
            private String jobTitle;
            private JobLocationDto jobLocation;
            private JobCompanyDto jobCompany;
        }