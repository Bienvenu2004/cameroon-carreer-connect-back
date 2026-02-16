package com.hostdesign24.jobportal.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RecruiterProfileDto {
    private UUID id;
    private String companyName;
    private String description;
    private String profilePhoto;
}
