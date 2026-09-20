package com.medisphere.monitoring;

import com.medisphere.audit.AuditService;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.kafka.event.VitalEvent;
import com.medisphere.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alertRepository;
    private final MonitoringService monitoringService;
    private final AuditService auditService;

    public List<Alert> process(VitalEvent event) {
        List<Alert> created = new java.util.ArrayList<>();
        for (MonitoringEvaluation evaluation : monitoringService.evaluate(event)) {
            Alert alert = alertRepository.findFirstByPatientIdAndAlertTypeAndVitalTypeAndStatus(
                    event.getPatientId(), evaluation.rule().alertType(), evaluation.rule().vitalType(), AlertStatus.ACTIVE)
                    .orElseGet(() -> create(event, evaluation));
            if (alert.getId() != null && created.stream().noneMatch(existing -> existing.getId().equals(alert.getId()))) {
                created.add(alert);
            }
        }
        return created;
    }

    private Alert create(VitalEvent event, MonitoringEvaluation evaluation) {
        MonitoringRule rule = evaluation.rule();
        Alert alert = Alert.builder()
                .id(UUID.randomUUID().toString())
                .patientId(event.getPatientId())
                .alertType(rule.alertType())
                .vitalType(rule.vitalType())
                .observedValue(evaluation.observedValue())
                .threshold(rule.threshold())
                .operator(rule.operator())
                .severity(rule.severity())
                .message(evaluation.message())
                .source(event.getSource())
                .status(AlertStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        Alert saved = alertRepository.save(alert);
        auditService.record(AuditService.Actions.ALERT_CREATED, "Alert", saved.getId(), saved.getPatientId(),
                AuditService.Outcomes.SUCCESS, saved.getMessage());
        return saved;
    }

    public Page<Alert> patientAlerts(String patientId, Pageable pageable) {
        return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
    }

    public Page<Alert> activePatientAlerts(String patientId, Pageable pageable) {
        return alertRepository.findByPatientIdAndStatusInOrderByCreatedAtDesc(
            patientId, List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED), pageable);
    }

    public Page<Alert> history(String patientId, Pageable pageable) {
        return alertRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, AlertStatus.RESOLVED, pageable);
    }

    public Alert get(String id) {
        return alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }

    public Alert acknowledge(String id) {
        Alert alert = get(id);
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new InvalidAlertStateException("Only ACTIVE alerts can be acknowledged");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(actor());
        alert.setAcknowledgedAt(Instant.now());
        Alert saved = alertRepository.save(alert);
        auditService.record(AuditService.Actions.ALERT_ACKNOWLEDGED, "Alert", saved.getId(), saved.getPatientId(),
                AuditService.Outcomes.SUCCESS, "Alert acknowledged");
        return saved;
    }

    public Alert resolve(String id) {
        Alert alert = get(id);
        if (alert.getStatus() != AlertStatus.ACTIVE && alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new InvalidAlertStateException("Only ACTIVE or ACKNOWLEDGED alerts can be resolved");
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(actor());
        alert.setResolvedAt(Instant.now());
        Alert saved = alertRepository.save(alert);
        auditService.record(AuditService.Actions.ALERT_RESOLVED, "Alert", saved.getId(), saved.getPatientId(),
                AuditService.Outcomes.SUCCESS, "Alert resolved");
        return saved;
    }

    private String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
