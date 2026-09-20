package com.medisphere.risk;

import jakarta.validation.constraints.NotBlank;

public record RiskPredictionRequest(
        @NotBlank String patientId,
        @NotBlank String modelType,
        String source
) {
    public static RiskPredictionRequest forCardiovascular(String patientId) {
        return new RiskPredictionRequest(patientId, "CARDIOVASCULAR", "backend");
    }

    public static RiskPredictionRequest forDiabetes(String patientId) {
        return new RiskPredictionRequest(patientId, "DIABETES", "backend");
    }
}
