package com.medisphere.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published when a HealthTwin is created or updated.
 * M4 (Careplan) and M3 (Monitoring) will subscribe to this topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwinUpdateEvent {

    private String eventId;
    private String schemaVersion; // e.g., "1.0"
    private String patientId;
    private String twinId;
    private String updateType; // e.g., "CREATED", "VITALS_UPDATED", "FHIR_SYNCED", "LABS_UPDATED"
    private double completenessPercentage;
    private Instant timestamp;
}
