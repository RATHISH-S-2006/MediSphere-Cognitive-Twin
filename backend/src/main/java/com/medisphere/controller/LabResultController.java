package com.medisphere.controller;

import com.medisphere.dto.LabResultDtos;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.repository.LabResultRepository;
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
@RequestMapping("/api/labs")
@RequiredArgsConstructor
public class LabResultController {

    private final LabResultRepository labResultRepository;
    private final ApiDtoMapper dtoMapper;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<LabResultDtos.LabResultResponse> listLabResults(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size);
        return labResultRepository.findByPatientIdOrderByCollectedAtDesc(patientId, pageable)
                .map(dtoMapper::toLabResultResponse);
    }

    @GetMapping("/{patientId}/latest")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public LabResultDtos.LabResultResponse latestLabResult(@PathVariable String patientId) {
        return labResultRepository.findByPatientIdOrderByCollectedAtDesc(patientId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(dtoMapper::toLabResultResponse)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult", patientId));
    }
}