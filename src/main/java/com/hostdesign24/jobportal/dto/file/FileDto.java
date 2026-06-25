package com.hostdesign24.jobportal.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID id;
    private String name;
    private String url;
    private String type;
    private UUID ownerId;
    private Long size;
    private String ownerType;
}
