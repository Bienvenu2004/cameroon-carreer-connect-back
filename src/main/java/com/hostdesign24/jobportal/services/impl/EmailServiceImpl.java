package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
  private static final String LOGO_SRC = "cid:" + EmailSender.LOGO_CID;
  /** The templates name the logo variable both ways. */
  private static final String[] LOGO_VARIABLES = {"logo", "logoUrl"};

  private final TemplateEngine templateEngine;
  private final EmailSender service;

  @Override
  public void sendEmail(String to, String subject, String templateName, Map<String, Object> context) {
    // Callers pass a public logo URL; every template gets the copy embedded in
    // the message instead (see EmailSender.LOGO_CID).
    Map<String, Object> variables = new HashMap<>(context);
    for (String name : LOGO_VARIABLES) {
      variables.put(name, LOGO_SRC);
    }

    Context thymeleafContext = new Context();
    thymeleafContext.setVariables(variables);

    String htmlBody = templateEngine.process(templateName, thymeleafContext);

    service.sendEmailAsync(to, subject, htmlBody, toPlainText(htmlBody));
  }

  /**
   * Plain-text alternative derived from the rendered HTML, so the two parts can
   * never say different things. Block elements become line breaks and links keep
   * their target, since a verification link is useless without it.
   */
  static String toPlainText(String html) {
    Document doc = Jsoup.parse(html);
    doc.select("head, style, script, img").remove();
    for (Element link : doc.select("a[href]")) {
      String href = link.attr("href");
      if (href.startsWith("http") && !link.text().contains(href)) {
        link.appendText(" (" + href + ")");
      }
    }
    for (Element block : doc.select("p, div, tr, li, h1, h2, h3, h4, table")) {
      block.appendText("\n");
    }
    for (Element br : doc.select("br")) {
      br.after(new TextNode("\n"));
    }

    String text = doc.body().wholeText()
        .replace(' ', ' ')
        .replaceAll("[ \\t]+", " ")
        .replaceAll("(?m)^ +| +$", "")
        .replaceAll("\\n{3,}", "\n\n");
    return text.strip();
  }
}
