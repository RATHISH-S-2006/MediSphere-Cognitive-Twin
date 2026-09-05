package com.medisphere.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

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