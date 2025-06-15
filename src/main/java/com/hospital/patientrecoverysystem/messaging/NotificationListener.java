package com.hospital.patientrecoverysystem.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    @RabbitListener(queues = "patient.queue")
    public void listen(String message) {
        System.out.println("Notification to Physician: " + message);
    }
}