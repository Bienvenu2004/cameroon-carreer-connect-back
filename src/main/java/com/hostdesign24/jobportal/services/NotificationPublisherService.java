package com.hostdesign24.jobportal.services;

import com.hostdesign24.jobportal.dto.NotificationDto;
import com.hostdesign24.jobportal.dto.NotificationRequestDto;

public interface NotificationPublisherService {
    NotificationDto persistAndPublish(NotificationRequestDto request);
}
