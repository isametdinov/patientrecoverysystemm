package com.hospital.patientrecoverysystem.controller;

import com.hospital.patientrecoverysystem.entity.Vitals;
import com.hospital.patientrecoverysystem.repository.VitalsRepository;
import com.hospital.patientrecoverysystem.service.MonitoringService;
import com.hospital.patientrecoverysystem.service.VitalsService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class DashboardController {
    @Autowired
    private VitalsService vitalsService;

    @Autowired
    private VitalsRepository vitalsRepository;

    @Autowired
    private MonitoringService monitoringService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        switch (role) {
            case "PHYSICIAN":
                List<Vitals> emergencies = vitalsRepository.findByAlertStatus("Emergency");
                model.addAttribute("emergencies", emergencies);
                model.addAttribute("notifications", monitoringService.getNotifications());
                return "physician_dashboard";
            case "NURSE":
                List<Vitals> warnings = vitalsRepository.findByAlertStatus("Warning");
                model.addAttribute("warnings", warnings);
                return "nurse_dashboard";
            case "PATIENT":
                List<Vitals> patientVitals = vitalsRepository.findByPatientUsername(username);
                model.addAttribute("vitals", patientVitals);
                return "patient_dashboard";
            default:
                return "redirect:/login";
        }
    }

    @PostMapping("/patient/report-vitals")
    public String reportVitals(
            @RequestParam Double temperature,
            @RequestParam String bloodPressure,
            @RequestParam Integer heartRate,
            Authentication authentication,
            Model model) {
        String patientUsername = authentication.getName();
        String alertStatus = vitalsService.monitorPatientVitals(patientUsername, temperature, bloodPressure, heartRate);
        model.addAttribute("alertStatus", alertStatus);
        List<Vitals> patientVitals = vitalsRepository.findByPatientUsername(patientUsername);
        model.addAttribute("vitals", patientVitals);
        return "patient_dashboard";
    }

    @PostMapping("/patient/filter-vitals")
    public String filterVitals(
            @RequestParam String startDate,
            @RequestParam String endDate,
            Authentication authentication,
            Model model) {
        String patientUsername = authentication.getName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        List<Vitals> filteredVitals = vitalsRepository.findByPatientUsernameAndTimestampBetween(patientUsername, start, end);
        model.addAttribute("vitals", filteredVitals);
        return "patient_dashboard";
    }

    @GetMapping("/physician/export-vitals-txt")
    public ResponseEntity<ByteArrayResource> exportVitalsTxt() throws IOException {
        List<Vitals> emergencies = vitalsRepository.findByAlertStatus("Emergency");
        StringBuilder content = new StringBuilder();
        content.append("Emergency Vitals Report\n");
        content.append("Patient\tTimestamp\tTemperature\tBlood Pressure\tHeart Rate\tAlert Status\n");
        for (Vitals vital : emergencies) {
            content.append(String.format("%s\t%s\t%.1f\t%s\t%d\t%s\n",
                    vital.getPatientUsername(), vital.getTimestamp(), vital.getTemperature(),
                    vital.getBloodPressure(), vital.getHeartRate(), vital.getAlertStatus()));
        }

        ByteArrayResource resource = new ByteArrayResource(content.toString().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=emergencies.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @GetMapping("/physician/export-vitals-pdf")
    public ResponseEntity<ByteArrayResource> exportVitalsPdf() throws IOException {
        List<Vitals> emergencies = vitalsRepository.findByAlertStatus("Emergency");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(25, 750);
                contentStream.showText("Emergency Vitals Report");
                contentStream.newLineAtOffset(0, -20);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.showText("Patient  Timestamp  Temperature  Blood Pressure  Heart Rate  Alert Status");
                contentStream.newLineAtOffset(0, -15);
                for (Vitals vital : emergencies) {
                    contentStream.showText(String.format("%s  %s  %.1f°C  %s  %d bpm  %s",
                            vital.getPatientUsername(), vital.getTimestamp(), vital.getTemperature(),
                            vital.getBloodPressure(), vital.getHeartRate(), vital.getAlertStatus()));
                    contentStream.newLineAtOffset(0, -15);
                }
                contentStream.endText();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            ByteArrayResource resource = new ByteArrayResource(byteArrayOutputStream.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=emergencies.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        }
    }

    @GetMapping("/nurse/export-vitals-txt")
    public ResponseEntity<ByteArrayResource> exportWarningsTxt() throws IOException {
        List<Vitals> warnings = vitalsRepository.findByAlertStatus("Warning");
        StringBuilder content = new StringBuilder();
        content.append("Warning Vitals Report\n");
        content.append("Patient\tTimestamp\tTemperature\tBlood Pressure\tHeart Rate\tAlert Status\n");
        for (Vitals vital : warnings) {
            content.append(String.format("%s\t%s\t%.1f\t%s\t%d\t%s\n",
                    vital.getPatientUsername(), vital.getTimestamp(), vital.getTemperature(),
                    vital.getBloodPressure(), vital.getHeartRate(), vital.getAlertStatus()));
        }

        ByteArrayResource resource = new ByteArrayResource(content.toString().getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=warnings.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @GetMapping("/nurse/export-vitals-pdf")
    public ResponseEntity<ByteArrayResource> exportWarningsPdf() throws IOException {
        List<Vitals> warnings = vitalsRepository.findByAlertStatus("Warning");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(25, 750);
                contentStream.showText("Warning Vitals Report");
                contentStream.newLineAtOffset(0, -20);
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.showText("Patient  Timestamp  Temperature  Blood Pressure  Heart Rate  Alert Status");
                contentStream.newLineAtOffset(0, -15);
                for (Vitals vital : warnings) {
                    contentStream.showText(String.format("%s  %s  %.1f°C  %s  %d bpm  %s",
                            vital.getPatientUsername(), vital.getTimestamp(), vital.getTemperature(),
                            vital.getBloodPressure(), vital.getHeartRate(), vital.getAlertStatus()));
                    contentStream.newLineAtOffset(0, -15);
                }
                contentStream.endText();
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            ByteArrayResource resource = new ByteArrayResource(byteArrayOutputStream.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=warnings.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        }
    }
}