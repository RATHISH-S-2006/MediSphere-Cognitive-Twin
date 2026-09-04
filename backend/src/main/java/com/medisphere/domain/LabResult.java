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
 * Laboratory result stored in its own collection.
 * NOT embedded in HealthTwin for scalability.
 * M2 will use these for AI model feature extraction (e.g., HbA1c for diabetes prediction).
 */
@Document(collection = "lab_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "patient_time_idx", def = "{'patientId': 1, 'collectedAt': -1}"),
    @CompoundIndex(name = "patient_type_idx", def = "{'patientId': 1, 'testCode': 1}")
})
public class LabResult {

    @Id
    private String id; // UUID

    @Indexed
    private String patientId;

    private String fhirObservationId; // FHIR Observation resource ID for deduplication

    private String testName;
    private String testCode; // e.g., LOINC code
    private String value;
    private String unit;
    private String referenceRange;
    private String interpretation; // e.g., "NORMAL", "HIGH", "LOW"

    private String category; // e.g., "CHEMISTRY", "HEMATOLOGY"
    private String sourceSystem;

    private Instant collectedAt;
    private Instant reportedAt;

    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;
}
