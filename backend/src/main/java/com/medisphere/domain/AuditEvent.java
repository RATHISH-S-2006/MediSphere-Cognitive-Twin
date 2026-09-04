package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * HIPAA-oriented audit event.
 * Records security-sensitive operations for compliance and forensic traceability.
 * 
 * NEVER log: passwords, tokens, client secrets, or unnecessary PHI.
 * Correlation IDs link this to the originating HTTP request and Kafka events.
 */
@Document(collection = "audit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "actor_time_idx", def = "{'actorId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "patient_time_idx", def = "{'patientId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "action_time_idx", def = "{'action': 1, 'timestamp': -1}")
})
public class AuditEvent {

    @Id
    private String auditId; // UUID

    @Indexed
    private Instant timestamp;

    private String actorId;   // User/system ID performing the action
    private String actorRole; // PATIENT, PROVIDER, ADMIN, SYSTEM

    private String action;        // e.g., "PATIENT_ACCESS", "CONSENT_REVOKE", "TWIN_ACCESS"
    private String resourceType;  // e.g., "Patient", "HealthTwin", "Consent"
    private String resourceId;    // ID of the accessed resource

    @Indexed
    private String patientId;     // Patient context (if applicable)

    private String outcome;       // "SUCCESS", "FAILURE", "DENIED"
    private String outcomeDetail; // Brief description - NO sensitive data

    private String correlationId; // Trace ID linking HTTP → Kafka → DB
    private String requestPath;   // HTTP path (sanitized, no query params with PHI)
    private String clientIp;      // Anonymized or masked in production

    @Builder.Default
    private String schemaVersion = "1.0";
}
