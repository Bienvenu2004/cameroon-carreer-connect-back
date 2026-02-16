package com.hostdesign24.jobportal;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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
