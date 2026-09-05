package com.medisphere.controller;

import com.medisphere.dto.HealthTwinDtos;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.service.HealthTwinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/twins")
@RequiredArgsConstructor
public class TwinController {

    private final HealthTwinService healthTwinService;
    private final ApiDtoMapper dtoMapper;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public HealthTwinDtos.HealthTwinResponse getTwin(@PathVariable String patientId) {
        return healthTwinService.findByPatientId(patientId)
                .map(dtoMapper::toHealthTwinResponse)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin", patientId));
    }

    @GetMapping("/{patientId}/completeness")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public HealthTwinDtos.TwinCompletenessResponse getCompleteness(@PathVariable String patientId) {
        healthTwinService.evaluateCompleteness(patientId);
        return healthTwinService.findByPatientId(patientId)
                .map(dtoMapper::toCompletenessResponse)
                .orElseThrow(() -> new ResourceNotFoundException("HealthTwin", patientId));
    }

    @PostMapping("/{patientId}/sync")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public HealthTwinDtos.TwinSyncResponse syncTwin(@PathVariable String patientId,
                                                   @RequestBody(required = false) @Valid HealthTwinDtos.TwinSyncRequest request) {
        return dtoMapper.toTwinSyncResponse(healthTwinService.createOrUpdateTwin(patientId));
    }
}