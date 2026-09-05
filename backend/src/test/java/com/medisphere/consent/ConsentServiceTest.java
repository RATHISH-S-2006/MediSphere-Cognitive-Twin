package com.medisphere.consent;

import com.medisphere.audit.AuditService;
import com.medisphere.domain.Consent;
import com.medisphere.exception.ConsentRequiredException;
import com.medisphere.repository.ConsentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ConsentService consentService;

    @Test
    void grantsConsentAndAuditsIt() {
        Consent saved = Consent.builder()
                .consentId("consent-1")
                .patientId("patient-1")
                .status(Consent.ConsentStatus.GRANTED)
                .build();
        when(consentRepository.save(any(Consent.class))).thenReturn(saved);

        Consent result = consentService.grantConsent("patient-1", "TREATMENT", "ALL_DATA", "provider-1", null);

        assertThat(result).isSameAs(saved);
        verify(auditService).record(AuditService.Actions.CONSENT_GRANT, "Consent", "consent-1",
                "patient-1", AuditService.Outcomes.SUCCESS, "Consent granted for purpose: TREATMENT");
    }

    @Test
    void verifiesActiveConsentAndRejectsExpiredConsent() {
        Consent active = Consent.builder()
                .consentId("consent-1")
                .patientId("patient-1")
                .status(Consent.ConsentStatus.GRANTED)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(consentRepository.findTopByPatientIdAndStatusOrderByGrantedAtDesc(
                "patient-1", Consent.ConsentStatus.GRANTED)).thenReturn(Optional.of(active));

        consentService.verifyConsent("patient-1");
        assertThat(consentService.hasActiveConsent("patient-1")).isTrue();

        Consent expired = Consent.builder()
                .consentId(active.getConsentId())
                .patientId(active.getPatientId())
                .status(active.getStatus())
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(consentRepository.findTopByPatientIdAndStatusOrderByGrantedAtDesc(
                "patient-1", Consent.ConsentStatus.GRANTED)).thenReturn(Optional.of(expired));

        assertThat(consentService.hasActiveConsent("patient-1")).isFalse();
        assertThatThrownBy(() -> consentService.verifyConsent("patient-1"))
                .isInstanceOf(ConsentRequiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsMissingConsent() {
        when(consentRepository.findTopByPatientIdAndStatusOrderByGrantedAtDesc(
                "patient-1", Consent.ConsentStatus.GRANTED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consentService.verifyConsent("patient-1"))
                .isInstanceOf(ConsentRequiredException.class);
        verify(auditService).record(AuditService.Actions.CONSENT_CHECK, "Consent", null,
                "patient-1", AuditService.Outcomes.DENIED, "No active consent found");
    }

    @Test
    void revokesActiveConsent() {
        Consent active = Consent.builder()
                .consentId("consent-1")
                .patientId("patient-1")
                .status(Consent.ConsentStatus.GRANTED)
                .build();
        when(consentRepository.findTopByPatientIdAndStatusOrderByGrantedAtDesc(
                "patient-1", Consent.ConsentStatus.GRANTED)).thenReturn(Optional.of(active));
        when(consentRepository.save(active)).thenReturn(active);

        Consent result = consentService.revokeConsent("patient-1", "provider-1", "withdrawn");

        assertThat(result.getStatus()).isEqualTo(Consent.ConsentStatus.REVOKED);
        assertThat(result.getRevokedBy()).isEqualTo("provider-1");
        assertThat(result.getRevokeReason()).isEqualTo("withdrawn");
    }
}