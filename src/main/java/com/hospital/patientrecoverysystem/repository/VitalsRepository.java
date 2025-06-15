package com.hospital.patientrecoverysystem.repository;

import com.hospital.patientrecoverysystem.entity.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VitalsRepository extends JpaRepository<Vitals, Long> {
    List<Vitals> findByPatientUsername(String patientUsername);

    @Query("SELECT v FROM Vitals v WHERE v.patientUsername = :username AND v.timestamp BETWEEN :start AND :end")
    List<Vitals> findByPatientUsernameAndTimestampBetween(
            @Param("username") String username,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Vitals> findByAlertStatus(String alertStatus);
}