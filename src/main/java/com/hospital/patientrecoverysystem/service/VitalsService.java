package com.hospital.patientrecoverysystem.service;

import com.hospital.patientrecoverysystem.entity.Vitals;
import com.hospital.patientrecoverysystem.repository.VitalsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VitalsService {
    private static final Logger logger = LoggerFactory.getLogger(VitalsService.class);

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private MonitoringService monitoringService;

    public String monitorPatientVitals(String patientUsername, Double temperature, String bloodPressure, Integer heartRate) {
        // Step 1: Define normal ranges
        final double TEMP_MIN = 36.0, TEMP_MAX = 37.5;
        final int BP_SYSTOLIC_MIN = 90, BP_SYSTOLIC_MAX = 120, BP_DIASTOLIC_MIN = 60, BP_DIASTOLIC_MAX = 80;
        final int HR_MIN = 60, HR_MAX = 100;

        // Step 2: Validate inputs
        if (temperature == null || bloodPressure == null || heartRate == null) {
            logger.error("Invalid input: temperature={}, bloodPressure={}, heartRate={}", temperature, bloodPressure, heartRate);
            return "Error";
        }

        // Parse blood pressure (e.g., "120/80")
        int systolic, diastolic;
        try {
            String[] bpValues = bloodPressure.split("/");
            systolic = Integer.parseInt(bpValues[0]);
            diastolic = Integer.parseInt(bpValues[1]);
        } catch (Exception e) {
            logger.error("Invalid blood pressure format: {}", bloodPressure);
            return "Error";
        }

        // Step 3: Initialize alertStatus
        String alertStatus = "Normal";

        // Step 4: Check temperature
        if (temperature > 38.0 || temperature < 35.0) {
            alertStatus = "Warning";
            if (temperature > 40.0 || temperature < 34.0) {
                alertStatus = "Emergency";
            }
        }

        // Step 5: Check blood pressure
        if (alertStatus.equals("Normal") || alertStatus.equals("Warning")) {
            if (systolic > 140 || systolic < 90 || diastolic > 90 || diastolic < 60) {
                alertStatus = "Warning";
                if (systolic > 180 || systolic < 80 || diastolic > 100 || diastolic < 50) {
                    alertStatus = "Emergency";
                }
            }
        }

        // Step 6: Check heart rate
        if (alertStatus.equals("Normal") || alertStatus.equals("Warning")) {
            if (heartRate > 100 || heartRate < 60) {
                alertStatus = "Warning";
                if (heartRate > 120 || heartRate < 40) {
                    alertStatus = "Emergency";
                }
            }
        }

        // Step 7: If Emergency, send RabbitMQ alert
        if (alertStatus.equals("Emergency")) {
            String message = String.format("Emergency for patient %s: Temp=%.1f°C, BP=%s, HR=%d",
                    patientUsername, temperature, bloodPressure, heartRate);
            monitoringService.sendPatientUpdate(patientUsername, message);
        } else if (alertStatus.equals("Warning")) {
            String message = String.format("Warning for patient %s: Temp=%.1f°C, BP=%s, HR=%d",
                    patientUsername, temperature, bloodPressure, heartRate);
            monitoringService.sendPatientUpdate(patientUsername, message);
        }

        // Step 8: Store in PostgreSQL
        Vitals vitals = new Vitals();
        vitals.setPatientUsername(patientUsername);
        vitals.setTemperature(temperature);
        vitals.setBloodPressure(bloodPressure);
        vitals.setHeartRate(heartRate);
        vitals.setAlertStatus(alertStatus);
        vitals.setTimestamp(LocalDateTime.now());
        vitalsRepository.save(vitals);

        // Step 9: Return alertStatus
        return alertStatus;
    }
}