package com.hostdesign24.jobportal.dto.userDevice;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BlockDeviceRequestDto {
    @NotBlank(message = "ID cannot be blank")
    private UUID id;
    private String reason;
    boolean block;
}
