package com.medisphere.controller;

import com.medisphere.consent.ConsentService;
import com.medisphere.domain.Consent;
import com.medisphere.dto.ConsentDtos;
import com.medisphere.mapper.ApiDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    private final ApiDtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#request.patientId())")
    public ConsentDtos.ConsentResponse createConsent(@RequestBody @Valid ConsentDtos.ConsentCreateRequest request) {
        Consent consent = consentService.grantConsent(
                request.patientId(),
                request.purpose(),
                request.scope(),
                request.grantedBy(),
                request.expiresAt());
        return dtoMapper.toConsentResponse(consent);
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public List<ConsentDtos.ConsentResponse> getConsentHistory(@PathVariable String patientId) {
        return consentService.getConsentHistory(patientId).stream()
                .map(dtoMapper::toConsentResponse)
                .toList();
    }

    @PostMapping("/{patientId}/revoke")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ConsentDtos.ConsentResponse revokeConsent(@PathVariable String patientId,
                                                     @RequestBody @Valid ConsentDtos.ConsentRevokeRequest request) {
        return dtoMapper.toConsentResponse(
                consentService.revokeConsent(patientId, request.revokedBy(), request.reason()));
    }

    @GetMapping("/{patientId}/verify")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ConsentDtos.ConsentVerifyResponse verifyConsent(@PathVariable String patientId) {
        List<Consent> history = consentService.getConsentHistory(patientId);
        Consent activeConsent = history.stream()
                .filter(Consent::isActive)
                .max(Comparator.comparing(Consent::getGrantedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        try {
            consentService.verifyConsent(patientId);
            return new ConsentDtos.ConsentVerifyResponse(
                    patientId,
                    true,
                    activeConsent != null ? activeConsent.getConsentId() : null,
                    null,
                    Instant.now());
        } catch (RuntimeException ex) {
            return new ConsentDtos.ConsentVerifyResponse(
                    patientId,
                    false,
                    activeConsent != null ? activeConsent.getConsentId() : null,
                    ex.getMessage(),
                    Instant.now());
        }
    }
}