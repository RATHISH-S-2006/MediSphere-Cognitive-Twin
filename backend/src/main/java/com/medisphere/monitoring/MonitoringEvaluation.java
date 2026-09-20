package com.medisphere.monitoring;

public record MonitoringEvaluation(MonitoringRule rule, double observedValue) {
    public String message() {
        return "%s %s configured monitoring threshold: observed %s, threshold %s."
                .formatted(displayName(rule.vitalType()), displayOperator(rule.operator()),
                        format(observedValue), format(rule.threshold()));
    }

    private static String displayName(VitalType type) {
        return switch (type) {
            case HEART_RATE -> "Heart rate";
            case SPO2 -> "SpO2";
            case TEMPERATURE -> "Temperature";
            case SYSTOLIC_BLOOD_PRESSURE -> "Systolic blood pressure";
            case DIASTOLIC_BLOOD_PRESSURE -> "Diastolic blood pressure";
        };
    }

    private static String displayOperator(MonitoringOperator operator) {
        return switch (operator) {
            case GREATER_THAN -> "above";
            case LESS_THAN -> "below";
            case GREATER_THAN_OR_EQUAL -> "at or above";
            case LESS_THAN_OR_EQUAL -> "at or below";
        };
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
