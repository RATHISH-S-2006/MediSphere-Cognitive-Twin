package com.medisphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class VitalsDtos {

    private VitalsDtos() {
    }

    public record VitalsCreateRequest(
            @NotBlank String patientId,
            @NotBlank @Size(max = 100) String eventId,
            @NotNull Integer heartRate,
            @NotNull Integer systolicBp,
            @NotNull Integer diastolicBp,
            @NotNull Double spo2,
            Double temperature,
            Integer respiratoryRate,
            @Size(max = 100) String deviceId,
            @NotBlank @Size(max = 100) String source,
            @NotNull Instant recordedAt
    ) {
    }

    public record VitalsUpdateRequest(
            Integer heartRate,
            Integer systolicBp,
            Integer diastolicBp,
            Double spo2,
            Double temperature,
            Integer respiratoryRate,
            String deviceId,
            String source,
            Instant recordedAt
    ) {
    }

    public record VitalsResponse(
            String id,
            String patientId,
            String eventId,
            Integer heartRate,
            Integer systolicBp,
            Integer diastolicBp,
            Double spo2,
            Double temperature,
            Integer respiratoryRate,
            boolean dataQualityValid,
            String dataQualityNote,
            String deviceId,
            String source,
            Instant recordedAt,
            Instant receivedAt,
            String schemaVersion,
            Instant createdAt
    ) {
    }
}