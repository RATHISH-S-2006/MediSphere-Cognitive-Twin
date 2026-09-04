package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Digital Health Twin - the core aggregate of a patient's health state.
 *
 * Design principles:
 * - Does NOT embed unlimited time-series vitals. Historical vitals are in the 'vitals' collection.
 * - Contains only the LATEST vitals snapshot for quick access.
 * - References lab results and FHIR resources by ID only.
 * - Includes data completeness tracking.
 *
 * Extension points for future milestones (DO NOT implement now):
 * - M2: riskPredictions field (List<String> riskPredictionIds) - reserved field slot present.
 * - M3: alertIds field (List<String> alertIds) - reserved field slot present.
 * - M4: careplanIds field (List<String> careplanIds) - reserved field slot present.
 */
@Document(collection = "health_twins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "patient_version_idx", def = "{'patientId': 1, 'modelVersion': -1}")
})
public class HealthTwin {

    @Id
    private String twinId; // UUID

    @Indexed(unique = true)
    private String patientId; // Reference to Patient.id

    private String modelVersion;

    // ---- Demographics snapshot (denormalized for fast twin rendering) ----
    private String patientFirstName;
    private String patientLastName;
    private String patientDateOfBirth;
    private String patientGender;

    // ---- Latest vitals snapshot (NOT the full history - just the most recent) ----
    private VitalsSnapshot latestVitals;

    // ---- References to related collections (NOT embedded arrays) ----
    @Builder.Default
    private List<String> recentLabResultIds = new ArrayList<>();    // last N lab result IDs

    @Builder.Default
    private List<String> fhirResourceIds = new ArrayList<>();        // synced FHIR resource IDs

    // ---- Consent reference ----
    private String activeConsentId;
    private String consentStatus; // e.g., "GRANTED", "REVOKED", "PENDING"

    // ---- Data completeness ----
    private double completenessPercentage;
    @Builder.Default
    private List<String> missingDataPoints = new ArrayList<>();

    // ---- Synchronization metadata ----
    private Instant lastFhirSyncAt;
    private Instant lastVitalsUpdateAt;

    // ---- Extension points for future milestones (reserved, not implemented) ----
    // M2 - Risk Prediction module will populate these
    @Builder.Default
    private List<String> riskPredictionIds = new ArrayList<>();

    // M3 - Alert module will populate these
    @Builder.Default
    private List<String> alertIds = new ArrayList<>();

    // M4 - Careplan module will populate these
    @Builder.Default
    private List<String> careplanIds = new ArrayList<>();

    // ---- Versioning ----
    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long documentVersion;

    /**
     * Embedded minimal vitals snapshot - only the LATEST reading.
     * Full history is stored in the 'vitals' collection.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalsSnapshot {
        private Integer heartRate;
        private Integer systolicBp;
        private Integer diastolicBp;
        private Double spo2;
        private Instant recordedAt;
        private String deviceId;
        private String source;
    }
}
