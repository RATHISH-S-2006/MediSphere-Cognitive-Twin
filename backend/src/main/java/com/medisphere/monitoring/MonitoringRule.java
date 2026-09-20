package com.medisphere.monitoring;

public record MonitoringRule(
        String alertType,
        VitalType vitalType,
        AlertSeverity severity,
        MonitoringOperator operator,
        double threshold,
        boolean enabled) {
}
