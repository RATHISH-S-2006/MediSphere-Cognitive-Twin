package com.medisphere.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LabResultDtos {

    private LabResultDtos() {
    }

    public record LabResultCreateRequest(
            @NotBlank String patientId,
            @Size(max = 100) String fhirObservationId,
            @NotBlank String testName,
            @NotBlank String testCode,
            @Size(max = 255) String value,
            @Size(max = 100) String unit,
            @Size(max = 255) String referenceRange,
            @Size(max = 50) String interpretation,
            @Size(max = 100) String category,
            @Size(max = 100) String sourceSystem,
            Instant collectedAt,
            Instant reportedAt
    ) {
    }

    public record LabResultResponse(
            String id,
            String patientId,
            String fhirObservationId,
            String testName,
            String testCode,
            String value,
            String unit,
            String referenceRange,
            String interpretation,
            String category,
            String sourceSystem,
            Instant collectedAt,
            Instant reportedAt,
            String schemaVersion,
            Instant createdAt
    ) {
    }
}