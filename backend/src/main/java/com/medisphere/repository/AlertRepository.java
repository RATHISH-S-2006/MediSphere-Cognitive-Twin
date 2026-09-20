package com.medisphere.repository;

import com.medisphere.monitoring.Alert;
import com.medisphere.monitoring.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.List;

public interface AlertRepository extends MongoRepository<Alert, String> {
    Page<Alert> findByPatientIdOrderByCreatedAtDesc(String patientId, Pageable pageable);
    Page<Alert> findByPatientIdAndStatusOrderByCreatedAtDesc(String patientId, AlertStatus status, Pageable pageable);
    Page<Alert> findByPatientIdAndStatusInOrderByCreatedAtDesc(String patientId, List<AlertStatus> statuses, Pageable pageable);
    Page<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);
    Optional<Alert> findFirstByPatientIdAndAlertTypeAndVitalTypeAndStatus(
            String patientId, String alertType, com.medisphere.monitoring.VitalType vitalType, AlertStatus status);
}
