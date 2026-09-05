package com.medisphere.validation;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.medisphere.kafka.event.VitalEvent;

class VitalsValidatorTest {

    private final VitalsValidator validator = new VitalsValidator();

    @Test
    void acceptsTechnicallyValidVitals() {
        VitalEvent event = VitalEvent.builder()
                .patientId("patient-1")
                .timestamp(Instant.now())
                .heartRate(72)
                .systolicBp(120)
                .diastolicBp(80)
                .spo2(98.0)
                .build();

        VitalsValidator.VitalsValidationResult result = validator.validate(event);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsMissingIdentityTimestampAndOutOfRangeValues() {
        VitalEvent event = VitalEvent.builder()
                .heartRate(301)
                .systolicBp(49)
                .diastolicBp(201)
                .spo2(101.0)
                .timestamp(Instant.now().plusSeconds(61))
                .build();

        VitalsValidator.VitalsValidationResult result = validator.validate(event);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).contains(
                "patientId is required",
                "timestamp cannot be in the future",
                "heartRate 301 is outside valid range [20-300]",
                "systolicBp 49 is outside valid range [50-300]",
                "diastolicBp 201 is outside valid range [20-200]",
                "spo2 101.0 is outside valid range [50.0-100.0]");
    }
}