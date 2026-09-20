package com.medisphere.risk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class MlPredictionClient {

    private final RestClient restClient;

    @Autowired
    public MlPredictionClient(
            @Value("${medisphere.ml.base-url}") String baseUrl,
            @Value("${medisphere.ml.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${medisphere.ml.read-timeout-ms:10000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    MlPredictionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public MlPredictionResponse predict(String patientId, String modelType, Map<String, Double> features) {
        MlPredictionRequest request = new MlPredictionRequest(patientId, modelType, features);
        try {
            MlPredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                        throw new MlServiceException("ML service returned HTTP " + clientResponse.getStatusCode().value());
                    })
                    .body(MlPredictionResponse.class);
            validateResponse(response, patientId, modelType);
            return response;
        } catch (MlServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[ML] Prediction request failed for patient={} modelType={}", patientId, modelType, ex);
            throw new MlServiceException("ML service is unavailable", ex);
        }
    }

    void validateResponse(MlPredictionResponse response, String patientId, String modelType) {
        if (response == null || !patientId.equals(response.patientId()) || !modelType.equals(response.modelType())
                || !Double.isFinite(response.riskScore()) || response.riskScore() < 0.0 || response.riskScore() > 1.0
                || !Set.of("LOW", "MODERATE", "HIGH").contains(response.riskCategory())
                || response.modelVersion() == null || response.modelVersion().isBlank()
                || response.generatedAt() == null || response.generatedAt().isBlank()
                || response.explanations() == null || response.explanations().isEmpty()) {
            throw new MlServiceException("ML service returned a malformed prediction");
        }
        try {
            Instant.parse(response.generatedAt());
        } catch (RuntimeException ex) {
            throw new MlServiceException("ML service returned a malformed prediction timestamp", ex);
        }
        for (MlExplanation explanation : response.explanations()) {
            if (explanation == null || explanation.feature() == null || explanation.feature().isBlank()
                    || !Double.isFinite(explanation.value()) || !Double.isFinite(explanation.shapValue())
                    || !Set.of("INCREASES_RISK", "DECREASES_RISK").contains(explanation.impact())) {
                throw new MlServiceException("ML service returned malformed explanation data");
            }
        }
    }

    public record MlPredictionRequest(String patientId, String modelType, Map<String, Double> features) {
    }

    public record MlPredictionResponse(
            String patientId,
            String modelType,
            double riskScore,
            String riskCategory,
            String modelVersion,
            String generatedAt,
            java.util.List<MlExplanation> explanations
    ) {
    }

    public record MlExplanation(String feature, double value, double shapValue, String impact) {
    }
}
