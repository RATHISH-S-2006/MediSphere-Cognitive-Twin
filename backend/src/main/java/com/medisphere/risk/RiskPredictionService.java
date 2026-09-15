package com.medisphere.risk;

import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.PatientRepository;
import com.medisphere.repository.RiskPredictionRepository;
import com.medisphere.repository.VitalsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskPredictionService {

    private final PatientRepository patientRepository;
    private final VitalsRepository vitalsRepository;
    private final LabResultRepository labResultRepository;
    private final RiskPredictionRepository riskPredictionRepository;
    private final MlPredictionClient mlPredictionClient;

    @PostConstruct
    public void init() {
        log.info("[RISK] Risk prediction service initialized with deterministic feature extraction");
    }

    public RiskPredictionResponse buildAndPersistPrediction(String patientId, RiskPredictionRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        Vitals latestVitals = vitalsRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Vitals", patientId));

        List<LabResult> labs = labResultRepository.findByPatientIdOrderByCollectedAtDesc(
                patientId, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        FeatureSummary summary = extractFeatureSummary(patient, latestVitals, labs);

        String modelType = normalizeModelType(request.modelType());
        Map<String, Double> features = toMlFeatures(modelType, summary);
        MlPredictionClient.MlPredictionResponse prediction = mlPredictionClient.predict(patientId, modelType, features);
        Instant generatedAt = Instant.parse(prediction.generatedAt());

        RiskPrediction riskPrediction = RiskPrediction.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .modelType(modelType)
                .riskScore(prediction.riskScore())
                .riskCategory(prediction.riskCategory())
                .modelVersion(prediction.modelVersion())
                .generatedAt(generatedAt)
                .source(request.source() == null ? "backend" : request.source())
                .explanations(prediction.explanations().stream()
                        .map(exp -> new RiskPredictionExplanation(exp.feature(), exp.feature(), exp.value(), exp.shapValue(), exp.impact()))
                        .toList())
                .inputFeatures(features)
                .build();

        riskPrediction = riskPredictionRepository.save(riskPrediction);

        return new RiskPredictionResponse(
                patientId,
                modelType,
                riskPrediction.getRiskScore(),
                riskPrediction.getRiskCategory(),
                riskPrediction.getModelVersion(),
                riskPrediction.getGeneratedAt(),
                riskPrediction.getExplanations().stream()
                        .map(exp -> new RiskPredictionExplanation(
                                exp.getFeature(), exp.getLabel(), exp.getValue(), exp.getShapValue(), exp.getImpact()))
                        .toList()
        );
    }

    public Page<RiskPrediction> listHistory(String patientId, Pageable pageable) {
        return riskPredictionRepository.findByPatientIdOrderByGeneratedAtDesc(patientId, pageable);
    }

    public RiskPrediction getLatest(String patientId, String modelType) {
        return riskPredictionRepository.findTopByPatientIdAndModelTypeOrderByGeneratedAtDesc(
                        patientId, normalizeModelType(modelType))
                .orElseThrow(() -> new ResourceNotFoundException("Risk prediction", patientId));
    }

    public FeatureSummary extractFeatureSummary(Patient patient, Vitals latestVitals, List<LabResult> labs) {
        String gender = patient.getGender() == null ? "unknown" : patient.getGender().toLowerCase(Locale.ROOT);
        double age = computeAge(patient.getDateOfBirth());
        double glucose = findNumericLabValue(labs, "Glucose", "GLUCOSE_FASTING", "GLUCOSE");
        double hba1c = findNumericLabValue(labs, "Hemoglobin A1c", "A1C", "HbA1c");
        double totalCholesterol = findNumericLabValue(labs, "Total Cholesterol", "Cholesterol", "TOTAL_CHOLESTEROL");
        double diabetesStatus = hba1c >= 6.5 || glucose >= 126 ? 1.0 : 0.0;

        return new FeatureSummary(
                age,
                gender,
                latestVitals.getSystolicBp() == null ? null : latestVitals.getSystolicBp().doubleValue(),
                latestVitals.getDiastolicBp() == null ? null : latestVitals.getDiastolicBp().doubleValue(),
                latestVitals.getHeartRate() == null ? null : latestVitals.getHeartRate().doubleValue(),
                null,
                diabetesStatus,
                null,
                totalCholesterol == 0.0 ? null : totalCholesterol,
                glucose == 0.0 ? null : glucose,
                hba1c == 0.0 ? null : hba1c,
                null
        );
    }

    private String normalizeModelType(String modelType) {
        if (modelType == null) return "CARDIOVASCULAR";
        String normalized = modelType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DIABETES", "DIABETES_COMPLICATION", "DIABETES-COMPLICATION" -> "DIABETES";
            case "CARDIO", "CARDIOVASCULAR_RISK" -> "CARDIOVASCULAR";
            default -> "CARDIOVASCULAR";
        };
    }

    private Map<String, Double> toMlFeatures(String modelType, FeatureSummary summary) {
        Map<String, Double> features = new LinkedHashMap<>();
        features.put("age", summary.age());
        features.put("sex", genderToSex(summary.gender()));
        if (summary.bmi() != null) features.put("bmi", summary.bmi());
        if (summary.systolicBloodPressure() != null) features.put("systolicBloodPressure", summary.systolicBloodPressure());
        if (summary.diastolicBloodPressure() != null) features.put("diastolicBloodPressure", summary.diastolicBloodPressure());
        if ("DIABETES".equals(modelType)) {
            if (summary.glucose() != null) features.put("glucose", summary.glucose());
            if (summary.hba1c() != null) features.put("hba1c", summary.hba1c());
            if (summary.diabetesDuration() != null) features.put("diabetesDuration", summary.diabetesDuration());
        } else {
            if (summary.heartRate() != null) features.put("heartRate", summary.heartRate());
            if (summary.smokingStatus() != null) features.put("smokingStatus", summary.smokingStatus());
            if (summary.diabetesStatus() != null) features.put("diabetesStatus", summary.diabetesStatus());
            if (summary.totalCholesterol() != null) features.put("totalCholesterol", summary.totalCholesterol());
        }
        return features;
    }

    private double genderToSex(String gender) {
        return "male".equalsIgnoreCase(gender) ? 1.0 : 0.0;
    }

    private double computeAge(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.length() < 4) return 45.0;
        try {
            int year = Integer.parseInt(dateOfBirth.substring(0, 4));
            int currentYear = Instant.now().atZone(java.time.ZoneId.systemDefault()).getYear();
            return Math.max(18.0, currentYear - year);
        } catch (Exception ex) {
            return 45.0;
        }
    }

    private double findNumericLabValue(List<LabResult> labs, String... candidates) {
        for (String candidate : candidates) {
            for (LabResult lab : labs) {
                if (lab == null) continue;
                boolean nameMatches = lab.getTestName() != null
                        && lab.getTestName().toLowerCase(Locale.ROOT).contains(candidate.toLowerCase(Locale.ROOT));
                boolean codeMatches = lab.getTestCode() != null && lab.getTestCode().equalsIgnoreCase(candidate);
                if (nameMatches || codeMatches) {
                    try {
                        return Double.parseDouble(lab.getValue());
                    } catch (Exception ignored) {
                        return 0.0;
                    }
                }
            }
        }
        return 0.0;
    }

    public record FeatureSummary(
            double age,
            String gender,
            Double systolicBloodPressure,
            Double diastolicBloodPressure,
            Double heartRate,
            Double smokingStatus,
            Double diabetesStatus,
            Double bmi,
            Double totalCholesterol,
            Double glucose,
            Double hba1c,
            Double diabetesDuration
    ) {
    }
}
