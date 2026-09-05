package com.medisphere.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.time.Instant;

public final class FhirDtos {

    private FhirDtos() {
    }

    public record FhirValidationRequest(
            @NotBlank String resourceJson
    ) {
    }

    public record FhirValidationResponse(
            boolean valid,
            String resourceType,
            String resourceId,
            List<String> errors
    ) {
    }

    public record FhirResourceResponse(
            String id,
            String fhirResourceId,
            String resourceType,
            String patientId,
            String fhirPatientRef,
            String sourceSystem,
            String version,
            boolean validationPassed,
            Instant lastUpdated
    ) {
    }

    public record FhirSyncRequest(
            @NotBlank String patientId,
            String fhirPatientId,
            String resourceType,
            String sourceSystem
    ) {
    }

    public record FhirSyncResponse(
            String patientId,
            String resourceType,
            String resourceId,
            boolean persisted,
            boolean published,
            String message
    ) {
    }
}