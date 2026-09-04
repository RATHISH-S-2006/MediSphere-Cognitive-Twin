package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Historical vitals measurement stored in its own time-series-friendly collection.
 * NOT embedded in HealthTwin to support scalable growth and M3 stream processing.
 *
 * M3 will consume these records via Kafka streams for anomaly detection.
 * M2 will use these for federated learning feature extraction.
 */
@Document(collection = "vitals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "patient_time_idx", def = "{'patientId': 1, 'recordedAt': -1}"),
    @CompoundIndex(name = "patient_source_idx", def = "{'patientId': 1, 'source': 1}")
})
public class Vitals {

    @Id
    private String id; // UUID

    @Indexed
    private String patientId; // Reference to Patient.id

    @Indexed(unique = true)
    private String eventId; // Kafka eventId - used for idempotency

    // ---- Vital Measurements ----
    private Integer heartRate;         // bpm
    private Integer systolicBp;        // mmHg
    private Integer diastolicBp;       // mmHg
    private Double spo2;               // % saturation
    private Double temperature;        // Celsius, if available
    private Integer respiratoryRate;   // breaths/min, if available

    // ---- Data quality flags ----
    private boolean dataQualityValid;
    @Builder.Default
    private String dataQualityNote = "OK";

    // ---- Source metadata ----
    private String deviceId;
    private String source; // e.g., "WEARABLE_SIMULATOR", "WEARABLE_DEVICE", "MANUAL"
    private Instant recordedAt;
    private Instant receivedAt;

    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;
}
