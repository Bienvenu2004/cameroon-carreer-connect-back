package com.hostdesign24.jobportal.config;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> {
            connector.setMaxPartCount(1000);
            
            // Other relevant Tomcat settings
            connector.setProperty("maxPostSize", String.valueOf(2L * 1024 * 1024 * 1024)); // 2GB
            connector.setProperty("maxSwallowSize", String.valueOf(2L * 1024 * 1024 * 1024)); // 2GB
        });
    }
}
