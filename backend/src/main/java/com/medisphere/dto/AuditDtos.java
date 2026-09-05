package com.medisphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditEventResponse(
            String auditId,
            Instant timestamp,
            String actorId,
            String actorRole,
            String action,
            String resourceType,
            String resourceId,
            String patientId,
            String outcome,
            String outcomeDetail,
            String correlationId,
            String requestPath,
            String clientIp,
            String schemaVersion
    ) {
    }

    public record AuditQueryRequest(
            String patientId,
            String actorId,
            String action,
            @Size(max = 100) String resourceType,
            Integer page,
            Integer size
    ) {
    }

    public record AuditRecordRequest(
            @NotBlank String action,
            @Size(max = 100) String resourceType,
            String resourceId,
            String patientId,
            @NotBlank String outcome,
            @Size(max = 255) String detail
    ) {
    }
}