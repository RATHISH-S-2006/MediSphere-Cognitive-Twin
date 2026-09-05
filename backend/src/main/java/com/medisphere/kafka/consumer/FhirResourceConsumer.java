package com.medisphere.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.AuditService;
import com.medisphere.domain.FHIRResource;
import com.medisphere.kafka.event.FhirResourceEvent;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.service.HealthTwinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirResourceConsumer {

    private final ObjectMapper objectMapper;
    private final FHIRResourceRepository fhirResourceRepository;
    private final HealthTwinService healthTwinService;
    private final AuditService auditService;

    @KafkaListener(topics = "medisphere.fhir.resources", groupId = "medisphere-group")
    public void consume(String message) {
        try {
            FhirResourceEvent event = objectMapper.readValue(message, FhirResourceEvent.class);
            if (fhirResourceRepository.existsByFhirResourceIdAndResourceType(event.getFhirResourceId(), event.getResourceType())) {
                log.debug("[KAFKA] Duplicate FHIR event ignored eventId={} resourceType={} resourceId={}",
                        event.getEventId(), event.getResourceType(), event.getFhirResourceId());
                return;
            }

            fhirResourceRepository.save(FHIRResource.builder()
                    .id(UUID.randomUUID().toString())
                    .fhirResourceId(event.getFhirResourceId())
                    .resourceType(event.getResourceType())
                    .patientId(event.getPatientId())
                    .sourceSystem(event.getSourceSystem())
                    .validationPassed(true)
                    .validationNote("consumed from Kafka")
                    .rawFhirJson(event.getRawFhirJson())
                    .receivedAt(Instant.now())
                    .lastUpdated(Instant.now())
                    .schemaVersion(event.getSchemaVersion())
                    .build());

            healthTwinService.createOrUpdateTwin(event.getPatientId());
            auditService.record(AuditService.Actions.FHIR_SYNC, "FHIRResource", event.getFhirResourceId(),
                    event.getPatientId(), AuditService.Outcomes.SUCCESS, "FHIR event consumed and twin synchronized");
        } catch (Exception ex) {
            log.error("[KAFKA] FHIR event processing failed; message will be retried and may be dead-lettered: {}",
                    ex.getMessage(), ex);
            throw new IllegalStateException("FHIR event processing failed", ex);
        }
    }
}