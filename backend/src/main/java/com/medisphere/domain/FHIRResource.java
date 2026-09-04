package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persisted FHIR resource record.
 * Tracks synchronized FHIR resources. The raw FHIR JSON is stored here
 * for reference, but the domain model is the source of truth for business logic.
 */
@Document(collection = "fhir_resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "patient_type_idx", def = "{'patientId': 1, 'resourceType': 1}"),
    @CompoundIndex(name = "fhir_id_type_idx", def = "{'fhirResourceId': 1, 'resourceType': 1}", unique = true)
})
public class FHIRResource {

    @Id
    private String id; // UUID - internal ID

    @Indexed
    private String fhirResourceId; // FHIR resource ID

    private String resourceType; // e.g., "Patient", "Observation", "Condition"

    @Indexed
    private String patientId; // Internal patient ID reference

    private String fhirPatientRef; // FHIR patient reference string (e.g., "Patient/123")
    private String sourceSystem;
    private String version; // FHIR resource version if available

    private boolean validationPassed;
    private String validationNote;

    // Raw FHIR JSON stored for audit/interoperability but NOT used for business logic
    private String rawFhirJson;

    private Instant receivedAt;
    private Instant lastUpdated;

    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
