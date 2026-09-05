package com.medisphere.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record PatientCreateRequest(
            @NotBlank @Size(max = 100) String fhirPatientId,
            @Size(max = 100) String mrn,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String dateOfBirth,
            @NotBlank @Size(max = 30) String gender,
            @Email @Size(max = 255) String email,
            @Size(max = 50) String phone,
            @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            Boolean active,
            @Size(max = 100) String sourceSystem,
            List<String> providerIds
    ) {
    }

    public record PatientUpdateRequest(
            @Size(max = 100) String mrn,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String dateOfBirth,
            @Size(max = 30) String gender,
            @Email @Size(max = 255) String email,
            @Size(max = 50) String phone,
            @Size(max = 255) String addressLine1,
            @Size(max = 255) String addressLine2,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,
            Boolean active,
            @Size(max = 100) String sourceSystem,
            List<String> providerIds
    ) {
    }

    public record PatientSummaryResponse(
            String id,
            String fhirPatientId,
            String mrn,
            String firstName,
            String lastName,
            String dateOfBirth,
            String gender,
            boolean active,
            String sourceSystem
    ) {
    }

    public record PatientDetailResponse(
            String id,
            String fhirPatientId,
            String mrn,
            String firstName,
            String lastName,
            String dateOfBirth,
            String gender,
            String email,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String postalCode,
            String country,
            boolean active,
            String sourceSystem,
            List<String> providerIds,
            String schemaVersion,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}