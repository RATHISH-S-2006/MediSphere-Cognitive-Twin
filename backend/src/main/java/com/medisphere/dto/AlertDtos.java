package com.medisphere.dto;

import com.medisphere.monitoring.Alert;
import com.medisphere.monitoring.AlertSeverity;
import com.medisphere.monitoring.AlertStatus;
import com.medisphere.monitoring.MonitoringOperator;
import com.medisphere.monitoring.VitalType;

import java.time.Instant;

public final class AlertDtos {
    private AlertDtos() {}

    public record AlertResponse(
            String id, String patientId, String alertType, VitalType vitalType,
            double observedValue, double threshold, MonitoringOperator operator,
            AlertSeverity severity, String message, String source, Instant createdAt,
            AlertStatus status, String acknowledgedBy, Instant acknowledgedAt,
            String resolvedBy, Instant resolvedAt) {
        public static AlertResponse from(Alert alert) {
            return new AlertResponse(alert.getId(), alert.getPatientId(), alert.getAlertType(), alert.getVitalType(),
                    alert.getObservedValue(), alert.getThreshold(), alert.getOperator(), alert.getSeverity(),
                    alert.getMessage(), alert.getSource(), alert.getCreatedAt(), alert.getStatus(),
                    alert.getAcknowledgedBy(), alert.getAcknowledgedAt(), alert.getResolvedBy(), alert.getResolvedAt());
        }
    }
}
