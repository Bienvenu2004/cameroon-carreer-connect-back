package com.hostdesign24.jobportal.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String email;
    private boolean active;
}
