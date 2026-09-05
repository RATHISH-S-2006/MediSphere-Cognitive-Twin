package com.medisphere.validation;

import com.medisphere.kafka.event.VitalEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Vitals data-quality validator.
 *
 * IMPORTANT - M1 scope only:
 * This validator checks DATA QUALITY only (physiologically possible ranges).
 * It does NOT detect anomalies, classify disease, generate alerts, or detect AFib.
 * Those clinical functions belong to Milestone 3.
 *
 * Valid ranges are technically valid bounds, NOT clinical normal ranges.
 */
@Component
public class VitalsValidator {

    // Technically valid (not clinically normal) bounds
    private static final int HR_MIN = 20;
    private static final int HR_MAX = 300;
    private static final int SYSTOLIC_MIN = 50;
    private static final int SYSTOLIC_MAX = 300;
    private static final int DIASTOLIC_MIN = 20;
    private static final int DIASTOLIC_MAX = 200;
    private static final double SPO2_MIN = 50.0;
    private static final double SPO2_MAX = 100.0;

    public VitalsValidationResult validate(VitalEvent event) {
        List<String> errors = new ArrayList<>();

        if (event.getPatientId() == null || event.getPatientId().isBlank()) {
            errors.add("patientId is required");
        }
        if (event.getTimestamp() == null) {
            errors.add("timestamp is required");
        } else if (event.getTimestamp().isAfter(Instant.now().plusSeconds(60))) {
            errors.add("timestamp cannot be in the future");
        }

        if (event.getHeartRate() != null &&
                (event.getHeartRate() < HR_MIN || event.getHeartRate() > HR_MAX)) {
            errors.add("heartRate " + event.getHeartRate() + " is outside valid range [" + HR_MIN + "-" + HR_MAX + "]");
        }
        if (event.getSystolicBp() != null &&
                (event.getSystolicBp() < SYSTOLIC_MIN || event.getSystolicBp() > SYSTOLIC_MAX)) {
            errors.add("systolicBp " + event.getSystolicBp() + " is outside valid range [" + SYSTOLIC_MIN + "-" + SYSTOLIC_MAX + "]");
        }
        if (event.getDiastolicBp() != null &&
                (event.getDiastolicBp() < DIASTOLIC_MIN || event.getDiastolicBp() > DIASTOLIC_MAX)) {
            errors.add("diastolicBp " + event.getDiastolicBp() + " is outside valid range [" + DIASTOLIC_MIN + "-" + DIASTOLIC_MAX + "]");
        }
        if (event.getSpo2() != null &&
                (event.getSpo2() < SPO2_MIN || event.getSpo2() > SPO2_MAX)) {
            errors.add("spo2 " + event.getSpo2() + " is outside valid range [" + SPO2_MIN + "-" + SPO2_MAX + "]");
        }

        return errors.isEmpty()
                ? VitalsValidationResult.valid()
                : VitalsValidationResult.invalid(errors);
    }

    public record VitalsValidationResult(boolean isValid, List<String> errors) {
        public static VitalsValidationResult valid() {
            return new VitalsValidationResult(true, List.of());
        }
        public static VitalsValidationResult invalid(List<String> errors) {
            return new VitalsValidationResult(false, errors);
        }
    }
}
