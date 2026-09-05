package com.medisphere.controller;

import com.medisphere.dto.AuditDtos;
import com.medisphere.mapper.ApiDtoMapper;
import com.medisphere.repository.AuditEventRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final ApiDtoMapper dtoMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    public Page<AuditDtos.AuditEventResponse> listAuditEvents(
            @RequestParam(required = false) String patientId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (patientId != null && !patientId.isBlank()) {
            return auditEventRepository.findByPatientIdOrderByTimestampDesc(patientId, pageable)
                    .map(dtoMapper::toAuditEventResponse);
        }
        return auditEventRepository.findAllByOrderByTimestampDesc(pageable)
                .map(dtoMapper::toAuditEventResponse);
    }
}