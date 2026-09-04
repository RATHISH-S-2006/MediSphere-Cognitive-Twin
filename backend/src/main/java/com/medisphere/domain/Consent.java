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
 * Patient consent record.
 * HIPAA-oriented consent management.
 * M2/M3/M4 modules must check consent before accessing patient data.
 * ConsentStatus is enforced server-side - frontend checkboxes alone are NOT sufficient.
 */
@Document(collection = "consents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "patient_status_idx", def = "{'patientId': 1, 'status': 1}"),
    @CompoundIndex(name = "patient_purpose_idx", def = "{'patientId': 1, 'purpose': 1}")
})
public class Consent {

    @Id
    private String consentId; // UUID

    @Indexed
    private String patientId;

    private ConsentStatus status;

    private String purpose; // e.g., "TREATMENT", "RESEARCH", "OPERATION"
    private String scope;   // e.g., "ALL_DATA", "VITALS_ONLY", "LABS_ONLY"

    private String grantedBy; // Actor ID (patient or authorized representative)
    private Instant grantedAt;
    private Instant expiresAt;  // null means no expiry
    private Instant revokedAt;
    private String revokedBy;
    private String revokeReason;

    private String notes;

    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ConsentStatus {
        PENDING,
        GRANTED,
        REVOKED,
        EXPIRED
    }

    /**
     * Checks whether this consent is currently active and valid.
     * Used by ConsentService for server-side enforcement.
     */
    public boolean isActive() {
        if (status != ConsentStatus.GRANTED) return false;
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) return false;
        return true;
    }
}
