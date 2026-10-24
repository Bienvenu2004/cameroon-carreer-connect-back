package com.hostdesign24.jobportal.services;
import java.util.Map;

public interface EmailService {
  void sendEmail(String to, String subject, String templateName, Map<String, Object> context);

}
