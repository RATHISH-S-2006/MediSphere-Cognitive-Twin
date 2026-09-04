package com.medisphere.consent;

import com.medisphere.audit.AuditService;
import com.medisphere.domain.Consent;
import com.medisphere.exception.ConsentRequiredException;
import com.medisphere.repository.ConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side consent management service.
 * Consent is enforced here - NOT in the frontend.
 * Future milestones (M2 risk processing, M3 monitoring, M4 careplan) must call
 * verifyConsent() before accessing patient data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final AuditService auditService;

    public Consent grantConsent(String patientId, String purpose, String scope,
                                 String grantedBy, Instant expiresAt) {
        Consent consent = Consent.builder()
                .consentId(UUID.randomUUID().toString())
                .patientId(patientId)
                .status(Consent.ConsentStatus.GRANTED)
                .purpose(purpose)
                .scope(scope)
                .grantedBy(grantedBy)
                .grantedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        Consent saved = consentRepository.save(consent);
        auditService.record(AuditService.Actions.CONSENT_GRANT, "Consent", saved.getConsentId(),
                patientId, AuditService.Outcomes.SUCCESS, "Consent granted for purpose: " + purpose);
        return saved;
    }

    public Consent revokeConsent(String patientId, String revokedBy, String reason) {
        Optional<Consent> activeConsent = consentRepository
                .findTopByPatientIdAndStatusOrderByGrantedAtDesc(patientId, Consent.ConsentStatus.GRANTED);

        if (activeConsent.isEmpty()) {
            throw new IllegalStateException("No active consent found for patient: " + patientId);
        }

        Consent consent = activeConsent.get();
        consent.setStatus(Consent.ConsentStatus.REVOKED);
        consent.setRevokedAt(Instant.now());
        consent.setRevokedBy(revokedBy);
        consent.setRevokeReason(reason);

        Consent saved = consentRepository.save(consent);
        auditService.record(AuditService.Actions.CONSENT_REVOKE, "Consent", saved.getConsentId(),
                patientId, AuditService.Outcomes.SUCCESS, "Consent revoked by: " + revokedBy);
        return saved;
    }

    public List<Consent> getConsentHistory(String patientId) {
        return consentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /**
     * Verifies that the patient has active consent.
     * Throws ConsentRequiredException if not satisfied.
     * This is the primary enforcement point for all data access.
     */
    public void verifyConsent(String patientId) {
        Optional<Consent> activeConsent = consentRepository
                .findTopByPatientIdAndStatusOrderByGrantedAtDesc(patientId, Consent.ConsentStatus.GRANTED);

        if (activeConsent.isEmpty()) {
            auditService.record(AuditService.Actions.CONSENT_CHECK, "Consent", null,
                    patientId, AuditService.Outcomes.DENIED, "No active consent found");
            throw new ConsentRequiredException(patientId);
        }

        Consent consent = activeConsent.get();
        if (!consent.isActive()) {
            String reason = consent.getStatus() == Consent.ConsentStatus.REVOKED ? "revoked" : "expired";
            auditService.record(AuditService.Actions.CONSENT_CHECK, "Consent", consent.getConsentId(),
                    patientId, AuditService.Outcomes.DENIED, "Consent is " + reason);
            throw new ConsentRequiredException(patientId, reason);
        }

        auditService.record(AuditService.Actions.CONSENT_CHECK, "Consent", consent.getConsentId(),
                patientId, AuditService.Outcomes.SUCCESS, "Consent verified");
    }

    public boolean hasActiveConsent(String patientId) {
        return consentRepository
                .findTopByPatientIdAndStatusOrderByGrantedAtDesc(patientId, Consent.ConsentStatus.GRANTED)
                .map(Consent::isActive)
                .orElse(false);
    }
}
