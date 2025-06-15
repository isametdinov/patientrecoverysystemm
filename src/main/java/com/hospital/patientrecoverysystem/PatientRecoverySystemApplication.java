package com.hospital.patientrecoverysystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class PatientRecoverySystemApplication {
	private static final Logger logger = LoggerFactory.getLogger(PatientRecoverySystemApplication.class);

	public static void main(String[] args) {
		// Log startup timestamp with timezone
		ZoneId zoneId = ZoneId.of("Asia/Kolkata"); // Adjust to your timezone (e.g., +05:30)
		String startTime = ZonedDateTime.now(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
		logger.info("Starting Patient Recovery System at {}", startTime);

		// Basic initialization check (ping database connectivity)
		try {
			Class.forName("org.postgresql.Driver");
			logger.info("PostgreSQL driver loaded successfully.");
		} catch (ClassNotFoundException e) {
			logger.error("PostgreSQL driver not found. Check dependencies.", e);
		}

		// Run the Spring Boot application
		SpringApplication.run(PatientRecoverySystemApplication.class, args);

		logger.info("Patient Recovery System is running...");
	}
}