package com.medisphere.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class HealthTwinDtos {

    private HealthTwinDtos() {
    }

    public record LatestVitalsSnapshotDto(
            Integer heartRate,
            Integer systolicBp,
            Integer diastolicBp,
            Double spo2,
            Instant recordedAt,
            String deviceId,
            String source
    ) {
    }

    public record HealthTwinResponse(
            String twinId,
            String patientId,
            String modelVersion,
            String patientFirstName,
            String patientLastName,
            String patientDateOfBirth,
            String patientGender,
            @Valid LatestVitalsSnapshotDto latestVitals,
            List<String> recentLabResultIds,
            List<String> fhirResourceIds,
            String activeConsentId,
            String consentStatus,
            double completenessPercentage,
            List<String> missingDataPoints,
            Instant lastFhirSyncAt,
            Instant lastVitalsUpdateAt,
            String schemaVersion,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TwinSyncRequest(
            @NotBlank String patientId,
            @Size(max = 100) String triggerSource
    ) {
    }

    public record TwinSyncResponse(
            @NotBlank String patientId,
            @NotNull String twinId,
            double completenessPercentage,
            List<String> missingDataPoints,
            Instant synchronizedAt
    ) {
    }

    public record TwinCompletenessResponse(
            @NotBlank String patientId,
            @NotNull String twinId,
            double completenessPercentage,
            List<String> missingDataPoints,
            Instant evaluatedAt
    ) {
    }
}