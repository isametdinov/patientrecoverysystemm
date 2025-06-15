package com.hospital.patientrecoverysystem.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MonitoringService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final List<String> notifications = new ArrayList<>();

    public void sendPatientUpdate(String patientId, String message) {
        notifications.add(message);
        rabbitTemplate.convertAndSend("patient.exchange", "patient.routingkey", message);
    }

    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }
}