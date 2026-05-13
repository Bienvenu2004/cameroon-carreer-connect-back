package com.hostdesign24.jobportal;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableJpaAuditing wires the AuditingEntityListener registered on
 * BaseEntity to the SpringSecurityAuditorAwareImpl bean (declared in
 * SecurityConfig as "auditorAware"). Without this annotation, @CreatedBy
 * and @LastModifiedBy on BaseEntity silently stay null on every save —
 * the listener exists but JPA never invokes it.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class DigitalPlatformForJobSearchAndRecruitmentInCameroonApplication {

	public static void main(String[] args) {

		// 1. Manually load .env variables into System properties
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});


		SpringApplication.run(DigitalPlatformForJobSearchAndRecruitmentInCameroonApplication.class, args);
	}

}
