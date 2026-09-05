package com.medisphere.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.audit.AuditService;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.kafka.event.VitalEvent;
import com.medisphere.repository.PatientRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.service.HealthTwinService;
import com.medisphere.consent.ConsentService;
import com.medisphere.validation.VitalsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VitalsConsumer {

    private final ObjectMapper objectMapper;
    private final VitalsRepository vitalsRepository;
    private final PatientRepository patientRepository;
    private final ConsentService consentService;
    private final VitalsValidator vitalsValidator;
    private final HealthTwinService healthTwinService;
    private final AuditService auditService;

    @KafkaListener(topics = "medisphere.vitals", groupId = "medisphere-group")
    public void consume(String message) {
        try {
            VitalEvent event = objectMapper.readValue(message, VitalEvent.class);

            if (vitalsRepository.existsByEventId(event.getEventId())) {
                log.debug("[KAFKA] Duplicate vitals event ignored eventId={} patientId={}", event.getEventId(), event.getPatientId());
                return;
            }

            if (patientRepository.findById(event.getPatientId()).isEmpty()) {
                auditService.record(AuditService.Actions.VALIDATION_FAILURE, "Vitals", event.getEventId(),
                        event.getPatientId(), AuditService.Outcomes.DENIED, "Patient does not exist");
                throw new IllegalStateException("Patient does not exist: " + event.getPatientId());
            }

            if (!consentService.hasActiveConsent(event.getPatientId())) {
                auditService.record(AuditService.Actions.CONSENT_CHECK, "Vitals", event.getEventId(),
                        event.getPatientId(), AuditService.Outcomes.DENIED, "No active consent for vitals ingestion");
                throw new IllegalStateException("No active consent for patient: " + event.getPatientId());
            }

            VitalsValidator.VitalsValidationResult validation = vitalsValidator.validate(event);
            if (!validation.isValid()) {
                auditService.record(AuditService.Actions.VALIDATION_FAILURE, "Vitals", event.getEventId(),
                        event.getPatientId(), AuditService.Outcomes.DENIED, String.join("; ", validation.errors()));
                throw new IllegalStateException("Invalid vitals event: " + String.join("; ", validation.errors()));
            }

            Patient patient = patientRepository.findById(event.getPatientId()).orElseThrow();
            Vitals vitals = Vitals.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId(event.getPatientId())
                    .eventId(event.getEventId())
                    .heartRate(event.getHeartRate())
                    .systolicBp(event.getSystolicBp())
                    .diastolicBp(event.getDiastolicBp())
                    .spo2(event.getSpo2())
                    .temperature(event.getTemperature())
                    .respiratoryRate(event.getRespiratoryRate())
                    .dataQualityValid(true)
                    .dataQualityNote("consumed from Kafka")
                    .deviceId(event.getDeviceId())
                    .source(event.getSource())
                    .recordedAt(event.getTimestamp())
                    .receivedAt(Instant.now())
                    .schemaVersion(event.getSchemaVersion())
                    .build();

            vitalsRepository.save(vitals);
            healthTwinService.updateLatestVitals(patient.getId(), vitals);
            auditService.record(AuditService.Actions.VITALS_ACCESS, "Vitals", vitals.getId(),
                    event.getPatientId(), AuditService.Outcomes.SUCCESS, "Vitals event consumed and twin updated");
        } catch (Exception ex) {
            log.error("[KAFKA] Vitals event processing failed; message will be retried and may be dead-lettered: {}",
                    ex.getMessage(), ex);
            throw new IllegalStateException("Vitals event processing failed", ex);
        }
    }
}