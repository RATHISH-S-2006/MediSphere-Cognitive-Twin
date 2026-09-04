package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Patient domain entity.
 * Stores internal patient identity and demographics.
 * The FHIR Patient resource remains the canonical source of truth for interoperability.
 * Extended by HealthTwin to build the digital representation.
 *
 * Future milestones:
 * - M2 will use this as the identity anchor for RiskPrediction and FLModel.
 * - M3 will use this for real-time monitoring and Alert correlation.
 * - M4 will use this for Careplan attribution.
 */
@Document(collection = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    private String id; // UUID - internal stable identifier

    @Indexed(unique = true)
    private String fhirPatientId; // FHIR resource ID

    @Indexed
    private String mrn; // Medical Record Number if available

    private String firstName;
    private String lastName;
    private String dateOfBirth; // ISO-8601 date string
    private String gender;

    // Contact information
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private boolean active;
    private String sourceSystem; // e.g., "HAPI_FHIR_LOCAL", "EHR_EPIC"

    // Schema versioning for future document evolution
    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Optimistic locking for concurrent updates
    @Version
    private Long documentVersion;

    // Provider associations - IDs only (no embedded objects to keep doc size bounded)
    @Builder.Default
    private List<String> providerIds = new ArrayList<>();
}
