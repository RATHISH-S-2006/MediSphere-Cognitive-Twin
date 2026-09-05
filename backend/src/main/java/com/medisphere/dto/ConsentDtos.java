package com.medisphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ConsentDtos {

    private ConsentDtos() {
    }

    public record ConsentCreateRequest(
            @NotBlank String patientId,
            @NotBlank @Size(max = 100) String purpose,
            @NotBlank @Size(max = 100) String scope,
            @NotBlank @Size(max = 100) String grantedBy,
            Instant expiresAt,
            @Size(max = 255) String notes
    ) {
    }

    public record ConsentRevokeRequest(
            @NotBlank @Size(max = 100) String revokedBy,
            @NotBlank @Size(max = 255) String reason
    ) {
    }

    public record ConsentResponse(
            String consentId,
            String patientId,
            String status,
            String purpose,
            String scope,
            String grantedBy,
            Instant grantedAt,
            Instant expiresAt,
            Instant revokedAt,
            String revokedBy,
            String revokeReason,
            String notes,
            String schemaVersion,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ConsentVerifyResponse(
            String patientId,
            boolean active,
            String consentId,
            String reason,
            Instant evaluatedAt
    ) {
    }
}