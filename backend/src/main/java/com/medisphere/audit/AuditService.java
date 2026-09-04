package com.medisphere.audit;

import com.medisphere.common.CorrelationIdFilter;
import com.medisphere.domain.AuditEvent;
import com.medisphere.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * HIPAA-oriented audit logging service.
 * Records security-sensitive operations for compliance and forensic tracing.
 *
 * NEVER log: passwords, tokens, client secrets.
 * Avoid logging unnecessary PHI.
 * Audit events are persisted asynchronously to avoid impacting request latency.
 *
 * Future milestones (M2, M3, M4) should call this service for their audit events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Async
    public void record(String action,
                       String resourceType,
                       String resourceId,
                       String patientId,
                       String outcome,
                       String detail) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actorId = extractActorId(auth);
            String actorRole = extractActorRole(auth);

            AuditEvent event = AuditEvent.builder()
                    .auditId(UUID.randomUUID().toString())
                    .timestamp(Instant.now())
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .patientId(patientId)
                    .outcome(outcome)
                    .outcomeDetail(detail)
                    .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                    .schemaVersion("1.0")
                    .build();

            auditEventRepository.save(event);
            log.info("[AUDIT] action={} resourceType={} resourceId={} patientId={} outcome={} correlationId={}",
                    action, resourceType, resourceId, patientId, outcome,
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
        } catch (Exception ex) {
            // Audit failures should NOT propagate to user-facing requests
            log.error("[AUDIT] Failed to record audit event: {}", ex.getMessage());
        }
    }

    private String extractActorId(Authentication auth) {
        if (auth == null) return "ANONYMOUS";
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return auth.getName();
    }

    private String extractActorRole(Authentication auth) {
        if (auth == null) return "ANONYMOUS";
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");
    }

    // Convenience constants for action names
    public static final class Actions {
        public static final String PATIENT_ACCESS       = "PATIENT_ACCESS";
        public static final String PATIENT_CREATE       = "PATIENT_CREATE";
        public static final String PATIENT_UPDATE       = "PATIENT_UPDATE";
        public static final String TWIN_ACCESS          = "TWIN_ACCESS";
        public static final String TWIN_SYNC            = "TWIN_SYNC";
        public static final String FHIR_ACCESS          = "FHIR_ACCESS";
        public static final String FHIR_SYNC            = "FHIR_SYNC";
        public static final String FHIR_VALIDATE        = "FHIR_VALIDATE";
        public static final String CONSENT_GRANT        = "CONSENT_GRANT";
        public static final String CONSENT_REVOKE       = "CONSENT_REVOKE";
        public static final String CONSENT_CHECK        = "CONSENT_CHECK";
        public static final String VITALS_ACCESS        = "VITALS_ACCESS";
        public static final String LABS_ACCESS          = "LABS_ACCESS";
        public static final String AUTH_SUCCESS         = "AUTH_SUCCESS";
        public static final String AUTH_FAILURE         = "AUTH_FAILURE";
        public static final String UNAUTHORIZED_ACCESS  = "UNAUTHORIZED_ACCESS";
        public static final String VALIDATION_FAILURE   = "VALIDATION_FAILURE";
        public static final String ADMIN_OPERATION      = "ADMIN_OPERATION";
    }

    public static final class Outcomes {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String DENIED  = "DENIED";
    }
}
