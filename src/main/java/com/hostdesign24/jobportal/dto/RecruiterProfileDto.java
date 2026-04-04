package com.hostdesign24.jobportal.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RecruiterProfileDto {
    private UUID id;
    private String companyName;
    private String description;
    private String profilePhoto;
}
