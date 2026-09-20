package com.medisphere.controller;

import com.medisphere.consent.ConsentService;
import com.medisphere.dto.AlertDtos;
import com.medisphere.monitoring.Alert;
import com.medisphere.monitoring.AlertService;
import com.medisphere.security.PatientAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;
    private final PatientAccessChecker patientAccessChecker;
    private final ConsentService consentService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<AlertDtos.AlertResponse> list(@PathVariable String patientId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return alertService.patientAlerts(patientId, pageRequest(page, size)).map(AlertDtos.AlertResponse::from);
    }

    @GetMapping("/{patientId}/active")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<AlertDtos.AlertResponse> active(@PathVariable String patientId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return alertService.activePatientAlerts(patientId, pageRequest(page, size)).map(AlertDtos.AlertResponse::from);
    }

    @GetMapping("/{patientId}/history")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<AlertDtos.AlertResponse> history(@PathVariable String patientId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return alertService.history(patientId, pageRequest(page, size)).map(AlertDtos.AlertResponse::from);
    }

    @GetMapping("/by-id/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN')")
    public AlertDtos.AlertResponse get(@PathVariable String id) {
        Alert alert = authorize(alertService.get(id));
        return AlertDtos.AlertResponse.from(alert);
    }

    @PostMapping("/by-id/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN')")
    public AlertDtos.AlertResponse acknowledge(@PathVariable String id) {
        authorize(alertService.get(id));
        return AlertDtos.AlertResponse.from(alertService.acknowledge(id));
    }

    @PostMapping("/by-id/{id}/resolve")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN')")
    public AlertDtos.AlertResponse resolve(@PathVariable String id) {
        authorize(alertService.get(id));
        return AlertDtos.AlertResponse.from(alertService.resolve(id));
    }

    private Alert authorize(Alert alert) {
        if (!patientAccessChecker.canAccess(alert.getPatientId())) {
            throw new AccessDeniedException("Alert access denied");
        }
        consentService.verifyConsent(alert.getPatientId());
        return alert;
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}
