package com.medisphere.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Versioned Kafka event for real-time vitals streaming.
 * Stable contract for M3 (anomaly detection) consumption.
 * schemaVersion must be bumped if payload shape changes in future milestones.
 *
 * IMPORTANT: M1 only validates data quality. 
 * M3 will add anomaly detection and alert generation on top of this event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalEvent {

    private String eventId;       // UUID - used for idempotency
    private String schemaVersion; // e.g., "1.0" - bump when schema changes
    private String patientId;
    private String deviceId;
    private String source;        // e.g., "WEARABLE_SIMULATOR", "WEARABLE_DEVICE"
    private Instant timestamp;

    // Vital measurements
    private Integer heartRate;
    private Integer systolicBp;
    private Integer diastolicBp;
    private Double spo2;
    private Double temperature;
    private Integer respiratoryRate;
}
