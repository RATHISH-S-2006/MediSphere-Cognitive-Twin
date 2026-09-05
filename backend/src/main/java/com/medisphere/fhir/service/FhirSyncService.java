package com.medisphere.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.domain.FHIRResource;
import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.fhir.mapper.FhirToDomainMapper;
import com.medisphere.fhir.validator.FhirResourceValidator;
import com.medisphere.fhir.validator.FhirValidationReport;
import com.medisphere.kafka.event.FhirResourceEvent;
import com.medisphere.kafka.event.KafkaTopics;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FHIR synchronization service.
 * Orchestrates the FHIR → Kafka → MongoDB → HealthTwin pipeline.
 *
 * Flow:
 * FHIR Server → FHIR Client → Validation → Kafka Event → Consumer → MongoDB → HealthTwin
 *
 * Implements idempotent processing: duplicate FHIR resources are skipped.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirSyncService {

    private final IGenericClient fhirClient;
    private final FhirContext fhirContext;
    private final FhirResourceValidator fhirValidator;
    private final FhirToDomainMapper fhirMapper;
    private final PatientRepository patientRepository;
    private final FHIRResourceRepository fhirResourceRepository;
    private final LabResultRepository labResultRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Syncs all patients from the FHIR server.
     * Creates or updates internal Patient records idempotently.
     */
    public List<String> syncPatients() {
        log.info("[FHIR] Starting patient synchronization from FHIR server");
        List<String> syncedIds = new ArrayList<>();

        try {
            Bundle bundle = fhirClient.search()
                    .forResource(org.hl7.fhir.r4.model.Patient.class)
                    .returnBundle(Bundle.class)
                    .execute();

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                org.hl7.fhir.r4.model.Patient fhirPatient =
                        (org.hl7.fhir.r4.model.Patient) entry.getResource();
                String rawJson = fhirContext.newJsonParser().encodeResourceToString(fhirPatient);

                FhirValidationReport report = fhirValidator.validateAndReport(rawJson);
                if (!report.isValid()) {
                    log.warn("[FHIR] Patient {} failed validation: {}", fhirPatient.getIdElement().getIdPart(), report.getErrors());
                    continue;
                }

                // Idempotent upsert: if patient already exists, skip creation
                String fhirId = fhirPatient.getIdElement().getIdPart();
                if (!patientRepository.existsByFhirPatientId(fhirId)) {
                    Patient patient = fhirMapper.mapPatient(fhirPatient);
                    patientRepository.save(patient);
                    log.info("[FHIR] Created patient with fhirId={}", fhirId);
                } else {
                    log.debug("[FHIR] Patient already exists fhirId={}, skipping creation", fhirId);
                }

                // Publish FHIR resource event
                publishFhirEvent(fhirId, "Patient", fhirId, rawJson, "HAPI_FHIR");
                syncedIds.add(fhirId);
            }
        } catch (Exception e) {
            log.error("[FHIR] Patient sync failed: {}", e.getMessage(), e);
        }

        log.info("[FHIR] Patient sync complete. Synced {} patients", syncedIds.size());
        return syncedIds;
    }

    public FhirValidationReport validateResource(String resourceJson) {
        return fhirValidator.validateAndReport(resourceJson);
    }

    /**
     * Syncs Observations (lab results/vitals from FHIR) for a given patient.
     */
    public void syncObservations(String internalPatientId, String fhirPatientId) {
        log.info("[FHIR] Syncing observations for patient={}", fhirPatientId);
        try {
            Bundle bundle = fhirClient.search()
                    .forResource(Observation.class)
                    .where(Observation.PATIENT.hasId(fhirPatientId))
                    .returnBundle(Bundle.class)
                    .execute();

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Observation obs = (Observation) entry.getResource();
                String fhirObsId = obs.getIdElement().getIdPart();
                String rawJson = fhirContext.newJsonParser().encodeResourceToString(obs);

                FhirValidationReport report = fhirValidator.validateAndReport(rawJson);
                if (!report.isValid()) {
                    log.warn("[FHIR] Observation {} failed validation: {}", fhirObsId, report.getErrors());
                    continue;
                }

                // Idempotent: skip if already persisted
                if (labResultRepository.existsByFhirObservationId(fhirObsId)) {
                    log.debug("[FHIR] Observation {} already exists, skipping", fhirObsId);
                    continue;
                }

                LabResult labResult = fhirMapper.mapObservationToLabResult(obs, internalPatientId);
                labResultRepository.save(labResult);
                publishFhirEvent(internalPatientId, "Observation", fhirObsId, rawJson, "HAPI_FHIR");
                log.info("[FHIR] Synced observation={} for patient={}", fhirObsId, internalPatientId);
            }
        } catch (Exception e) {
            log.error("[FHIR] Observation sync failed for patient={}: {}", fhirPatientId, e.getMessage());
        }
    }

    /**
     * Persists a FHIR resource record and publishes a Kafka event.
     * Idempotent: duplicate fhirResourceId + resourceType is skipped.
     */
    private void publishFhirEvent(String patientId, String resourceType, String fhirResourceId,
                                   String rawJson, String sourceSystem) {
        // Persist FHIR resource record (idempotent)
        if (!fhirResourceRepository.existsByFhirResourceIdAndResourceType(fhirResourceId, resourceType)) {
            FHIRResource resource = FHIRResource.builder()
                    .id(UUID.randomUUID().toString())
                    .fhirResourceId(fhirResourceId)
                    .resourceType(resourceType)
                    .patientId(patientId)
                    .sourceSystem(sourceSystem)
                    .validationPassed(true)
                    .rawFhirJson(rawJson)
                    .receivedAt(Instant.now())
                    .lastUpdated(Instant.now())
                    .build();
            fhirResourceRepository.save(resource);
        }

        // Publish Kafka event
        try {
            FhirResourceEvent event = FhirResourceEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .schemaVersion("1.0")
                    .patientId(patientId)
                    .fhirResourceId(fhirResourceId)
                    .resourceType(resourceType)
                    .sourceSystem(sourceSystem)
                    .timestamp(Instant.now())
                    .rawFhirJson(rawJson)
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.FHIR_RESOURCES, patientId, eventJson);
            log.debug("[KAFKA] Published FHIR event type={} patientId={}", resourceType, patientId);
        } catch (Exception e) {
            log.error("[KAFKA] Failed to publish FHIR event: {}", e.getMessage());
        }
    }
}
