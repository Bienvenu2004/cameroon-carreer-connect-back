package com.hostdesign24.jobportal.dto.file;

import com.hostdesign24.jobportal.dto.common.FilterDto;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FileFilterDto extends FilterDto {
    private String name;
    private String type;
    private String ownerType;
    private UUID ownerId;
    private Long sizeFile;
    private String relatedEntity;
}

