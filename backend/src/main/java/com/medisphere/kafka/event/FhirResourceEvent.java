package com.medisphere.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Versioned Kafka event for FHIR resource ingestion.
 * Consumed by FhirEventConsumer to persist resources and update the HealthTwin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FhirResourceEvent {

    private String eventId;
    private String schemaVersion; // e.g., "1.0"
    private String patientId;
    private String fhirResourceId;
    private String resourceType;
    private String sourceSystem;
    private Instant timestamp;
    private String rawFhirJson;
}
