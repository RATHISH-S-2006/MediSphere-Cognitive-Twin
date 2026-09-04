package com.medisphere.service;

import com.medisphere.domain.HealthTwin;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.kafka.event.KafkaTopics;
import com.medisphere.kafka.event.TwinUpdateEvent;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.repository.HealthTwinRepository;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Health Twin synchronization service.
 * Creates or updates a HealthTwin for a patient idempotently.
 * Calculates twin data completeness.
 *
 * Design:
 * - HealthTwin holds only LATEST vitals snapshot and ID references.
 * - Historical data lives in separate collections (vitals, lab_results).
 * - Completeness calculation is modular - M2/M3/M4 can add more criteria.
 *
 * Extension points:
 * - M2 will set riskPredictionIds on the twin.
 * - M3 will set alertIds on the twin.
 * - M4 will set careplanIds on the twin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthTwinService {

    private final HealthTwinRepository healthTwinRepository;
    private final PatientRepository patientRepository;
    private final LabResultRepository labResultRepository;
    private final FHIRResourceRepository fhirResourceRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates or updates a HealthTwin for the given patient.
     * Idempotent: calling multiple times produces the same result.
     */
    public HealthTwin createOrUpdateTwin(String patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        HealthTwin twin = healthTwinRepository.findByPatientId(patientId)
                .orElse(HealthTwin.builder()
                        .twinId(UUID.randomUUID().toString())
                        .patientId(patientId)
                        .modelVersion("1.0")
                        .build());

        // Update demographic snapshot
        twin.setPatientFirstName(patient.getFirstName());
        twin.setPatientLastName(patient.getLastName());
        twin.setPatientDateOfBirth(patient.getDateOfBirth());
        twin.setPatientGender(patient.getGender());

        // Reference recent lab result IDs
        List<String> labIds = labResultRepository
                .findByPatientIdOrderByCollectedAtDesc(patientId, PageRequest.of(0, 10))
                .getContent()
                .stream().map(lr -> lr.getId())
                .collect(Collectors.toList());
        twin.setRecentLabResultIds(labIds);

        // Reference FHIR resource IDs
        List<String> fhirIds = fhirResourceRepository
                .findByPatientId(patientId, PageRequest.of(0, 50))
                .getContent()
                .stream().map(fr -> fr.getId())
                .collect(Collectors.toList());
        twin.setFhirResourceIds(fhirIds);

        // Calculate completeness
        CompletenessResult completeness = calculateCompleteness(patient, twin);
        twin.setCompletenessPercentage(completeness.percentage());
        twin.setMissingDataPoints(completeness.missingPoints());

        twin.setLastFhirSyncAt(Instant.now());
        twin = healthTwinRepository.save(twin);

        // Publish twin update event
        publishTwinUpdateEvent(twin, "FHIR_SYNCED");

        log.info("[TWIN] Twin synced for patient={}, completeness={}%", patientId, completeness.percentage());
        return twin;
    }

    /**
     * Updates the HealthTwin's latest vitals snapshot.
     * Called by the VitalsConsumer after persisting a new vitals measurement.
     */
    public void updateLatestVitals(String patientId, Vitals vitals) {
        healthTwinRepository.findByPatientId(patientId).ifPresent(twin -> {
            HealthTwin.VitalsSnapshot snapshot = HealthTwin.VitalsSnapshot.builder()
                    .heartRate(vitals.getHeartRate())
                    .systolicBp(vitals.getSystolicBp())
                    .diastolicBp(vitals.getDiastolicBp())
                    .spo2(vitals.getSpo2())
                    .recordedAt(vitals.getRecordedAt())
                    .deviceId(vitals.getDeviceId())
                    .source(vitals.getSource())
                    .build();
            twin.setLatestVitals(snapshot);
            twin.setLastVitalsUpdateAt(Instant.now());

            // Recalculate completeness after vitals update
            patientRepository.findById(patientId).ifPresent(patient -> {
                CompletenessResult completeness = calculateCompleteness(patient, twin);
                twin.setCompletenessPercentage(completeness.percentage());
                twin.setMissingDataPoints(completeness.missingPoints());
            });

            healthTwinRepository.save(twin);
            publishTwinUpdateEvent(twin, "VITALS_UPDATED");
            log.debug("[TWIN] Vitals snapshot updated for patient={}", patientId);
        });
    }

    /**
     * Modular completeness calculation.
     * M2/M3/M4 can add additional criteria without restructuring this method.
     * Returns a percentage (0.0 - 100.0) and a list of missing data points.
     */
    public CompletenessResult calculateCompleteness(Patient patient, HealthTwin twin) {
        int total = 0;
        int present = 0;
        List<String> missing = new java.util.ArrayList<>();

        // Demographics criteria (4 points)
        total += 4;
        if (nonEmpty(patient.getFirstName()) && nonEmpty(patient.getLastName())) present++;
        else missing.add("demographics.name");
        if (nonEmpty(patient.getDateOfBirth())) present++;
        else missing.add("demographics.dateOfBirth");
        if (nonEmpty(patient.getGender())) present++;
        else missing.add("demographics.gender");
        if (nonEmpty(patient.getFhirPatientId())) present++;
        else missing.add("demographics.fhirId");

        // Vitals criteria (1 point)
        total += 1;
        if (twin.getLatestVitals() != null) present++;
        else missing.add("vitals.latestSnapshot");

        // Lab results (2 points)
        total += 2;
        long labCount = labResultRepository.findByPatientIdOrderByCollectedAtDesc(
                patient.getId(), PageRequest.of(0, 1)).getTotalElements();
        if (labCount > 0) { present++; present++; }
        else missing.add("labResults.any");

        // FHIR resources (3 points: Patient resource, at least one Observation, at least one other)
        total += 3;
        long fhirPatientCount = fhirResourceRepository
                .findByPatientIdAndResourceType(patient.getId(), "Patient", PageRequest.of(0, 1))
                .getTotalElements();
        if (fhirPatientCount > 0) present++;
        else missing.add("fhirResources.Patient");

        long fhirObsCount = fhirResourceRepository
                .findByPatientIdAndResourceType(patient.getId(), "Observation", PageRequest.of(0, 1))
                .getTotalElements();
        if (fhirObsCount > 0) present++;
        else missing.add("fhirResources.Observation");

        long otherFhirCount = fhirResourceRepository.findByPatientId(patient.getId(), PageRequest.of(0, 1))
                .getTotalElements();
        if (otherFhirCount >= 3) present++;
        else missing.add("fhirResources.sufficient");

        double pct = total > 0 ? Math.round((double) present / total * 1000.0) / 10.0 : 0.0;
        return new CompletenessResult(pct, missing);
    }

    public record CompletenessResult(double percentage, List<String> missingPoints) {}

    private boolean nonEmpty(String s) { return s != null && !s.isBlank(); }

    private void publishTwinUpdateEvent(HealthTwin twin, String updateType) {
        try {
            TwinUpdateEvent event = TwinUpdateEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .schemaVersion("1.0")
                    .patientId(twin.getPatientId())
                    .twinId(twin.getTwinId())
                    .updateType(updateType)
                    .completenessPercentage(twin.getCompletenessPercentage())
                    .timestamp(Instant.now())
                    .build();
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.TWIN_UPDATES, twin.getPatientId(), json);
        } catch (Exception e) {
            log.warn("[KAFKA] Failed to publish twin update event: {}", e.getMessage());
        }
    }
}
