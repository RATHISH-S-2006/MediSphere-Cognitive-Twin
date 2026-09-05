package com.medisphere.controller;

import com.medisphere.dto.FhirDtos;
import com.medisphere.fhir.validator.FhirValidationReport;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.service.HealthTwinService;
import com.medisphere.fhir.service.FhirSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fhir")
@RequiredArgsConstructor
public class FhirController {

    private final FhirSyncService fhirSyncService;
    private final ApiDtoMapper dtoMapper;
    private final HealthTwinService healthTwinService;

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN')")
    public FhirDtos.FhirValidationResponse validate(@RequestBody @Valid FhirDtos.FhirValidationRequest request) {
        FhirValidationReport report = fhirSyncService.validateResource(request.resourceJson());
        return dtoMapper.toFhirValidationResponse(report);
    }

    @PostMapping("/sync/patients")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public List<String> syncPatients() {
        return fhirSyncService.syncPatients();
    }

    @PostMapping("/sync/patients/observations")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public FhirDtos.FhirSyncResponse syncPatientObservations(
            @RequestParam String patientId,
            @RequestParam String fhirPatientId) {
        fhirSyncService.syncObservations(patientId, fhirPatientId);
        var twin = healthTwinService.createOrUpdateTwin(patientId);
        return dtoMapper.toFhirSyncResponse(patientId, "Observation", fhirPatientId, true, true,
                "FHIR observations synced and twin updated for patient " + patientId + " (completeness " + twin.getCompletenessPercentage() + ")");
    }
}