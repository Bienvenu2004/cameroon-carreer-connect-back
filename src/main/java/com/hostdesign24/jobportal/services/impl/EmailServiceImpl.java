package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
  private final TemplateEngine templateEngine;
  private final EmailSender service;

  @Override
  public void sendEmail(String to, String subject, String templateName, Map<String, Object> context) {
    Context thymeleafContext = new Context();
    thymeleafContext.setVariables(context);

    String htmlBody = templateEngine.process(templateName, thymeleafContext);

    service.sendEmailAsync(to, subject, htmlBody);
  }
}
