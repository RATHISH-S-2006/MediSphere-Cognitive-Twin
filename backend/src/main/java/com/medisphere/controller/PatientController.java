package com.medisphere.controller;

import com.medisphere.domain.Patient;
import com.medisphere.consent.ConsentService;
import com.medisphere.dto.PatientDtos;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.repository.PatientRepository;
import com.medisphere.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientRepository patientRepository;
    private final ApiDtoMapper dtoMapper;
    private final ConsentService consentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN')")
    public Page<PatientDtos.PatientSummaryResponse> listPatients(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        boolean patient = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PATIENT"));
        if (patient) {
            PatientDtos.PatientSummaryResponse summary = patientRepository.findById(authentication.getName())
                .map(dtoMapper::toPatientSummary)
                .orElse(null);
            return summary == null
                ? Page.empty(PageRequest.of(page, size))
                : new PageImpl<>(java.util.List.of(summary), PageRequest.of(0, 1), 1);
        }
        Pageable pageable = PageRequest.of(page, size);
        return patientRepository.findAll(pageable).map(dtoMapper::toPatientSummary);
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public PatientDtos.PatientDetailResponse getPatient(@PathVariable String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        consentService.verifyConsent(patientId);
        return dtoMapper.toPatientDetail(patient);
    }
}