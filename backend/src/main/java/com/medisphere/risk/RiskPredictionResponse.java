package com.medisphere.risk;

import java.time.Instant;
import java.util.List;

public record RiskPredictionResponse(
        String patientId,
        String modelType,
        double riskScore,
        String riskCategory,
        String modelVersion,
        Instant generatedAt,
        List<RiskPredictionExplanation> explanations
) {
}
