package com.medisphere.controller;

import com.medisphere.dto.VitalsDtos;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vitals")
@RequiredArgsConstructor
public class VitalsController {

    private final VitalsRepository vitalsRepository;
    private final ApiDtoMapper dtoMapper;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<VitalsDtos.VitalsResponse> listVitals(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size);
        return vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patientId, pageable)
                .map(dtoMapper::toVitalsResponse);
    }

    @GetMapping("/{patientId}/latest")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public VitalsDtos.VitalsResponse latestVitals(@PathVariable String patientId) {
        return vitalsRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId)
                .map(dtoMapper::toVitalsResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Vitals", patientId));
    }
}