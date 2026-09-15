package com.medisphere.risk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MlPredictionClientTest {

    private final MlPredictionClient client = new MlPredictionClient(null);

    @Test
    void rejectsMissingModelVersion() {
        assertMalformed(valid(null, "2026-09-15T00:00:00Z", 0.5, "patient-1", "CARDIOVASCULAR"));
    }

    @Test
    void rejectsInvalidScore() {
        assertMalformed(valid("model-v1", "2026-09-15T00:00:00Z", Double.NaN, "patient-1", "CARDIOVASCULAR"));
    }

    @Test
    void rejectsMismatchedPatient() {
        assertMalformed(valid("model-v1", "2026-09-15T00:00:00Z", 0.5, "patient-2", "CARDIOVASCULAR"));
    }

    @Test
    void rejectsMismatchedModelType() {
        assertMalformed(valid("model-v1", "2026-09-15T00:00:00Z", 0.5, "patient-1", "DIABETES"));
    }

    @Test
    void rejectsMalformedTimestamp() {
        assertMalformed(valid("model-v1", "not-a-timestamp", 0.5, "patient-1", "CARDIOVASCULAR"));
    }

    @Test
    void rejectsInvalidShapValue() {
        MlPredictionClient.MlPredictionResponse response = new MlPredictionClient.MlPredictionResponse(
                "patient-1", "CARDIOVASCULAR", 0.5, "MODERATE", "model-v1", "2026-09-15T00:00:00Z",
                List.of(new MlPredictionClient.MlExplanation("age", 40.0, Double.POSITIVE_INFINITY, "INCREASES_RISK")));

        assertMalformed(response);
    }

    private void assertMalformed(MlPredictionClient.MlPredictionResponse response) {
        assertThatThrownBy(() -> client.validateResponse(response, "patient-1", "CARDIOVASCULAR"))
                .isInstanceOf(MlServiceException.class);
    }

    private MlPredictionClient.MlPredictionResponse valid(
            String modelVersion, String generatedAt, double score, String patientId, String modelType) {
        return new MlPredictionClient.MlPredictionResponse(
                patientId, modelType, score, "MODERATE", modelVersion, generatedAt,
                List.of(new MlPredictionClient.MlExplanation("age", 40.0, 0.1, "INCREASES_RISK")));
    }
}