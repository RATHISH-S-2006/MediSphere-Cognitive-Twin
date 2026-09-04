package com.medisphere.kafka.event;

/**
 * Centralized Kafka topic name constants.
 * Topic names are configurable via application.yml but these constants
 * provide the canonical defaults and avoid scattered string literals.
 *
 * M3 will subscribe to VITALS topic for real-time anomaly detection.
 * M4 will subscribe to TWIN_UPDATES for careplan triggers.
 */
public final class KafkaTopics {

    private KafkaTopics() {} // Utility class

    public static final String FHIR_RESOURCES = "medisphere.fhir.resources";
    public static final String VITALS         = "medisphere.vitals";
    public static final String TWIN_UPDATES   = "medisphere.patient.twin-updates";
}
