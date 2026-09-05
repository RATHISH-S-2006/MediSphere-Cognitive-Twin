package com.medisphere.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medisphere.audit.AuditService;
import com.medisphere.consent.ConsentService;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.kafka.consumer.VitalsConsumer;
import com.medisphere.kafka.event.VitalEvent;
import com.medisphere.repository.PatientRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.service.HealthTwinService;
import com.medisphere.validation.VitalsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalsConsumerTest {

    @Mock private VitalsRepository vitalsRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ConsentService consentService;
    @Mock private HealthTwinService healthTwinService;
    @Mock private AuditService auditService;

    private VitalsConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new VitalsConsumer(objectMapper, vitalsRepository, patientRepository,
                consentService, new VitalsValidator(), healthTwinService, auditService);
    }

    @Test
    void persistsValidEventAndUpdatesTwin() throws Exception {
        VitalEvent event = validEvent();
        when(vitalsRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(Patient.builder().id("patient-1").build()));
        when(consentService.hasActiveConsent("patient-1")).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(vitalsRepository).save(any(Vitals.class));
        verify(healthTwinService).updateLatestVitals(org.mockito.ArgumentMatchers.eq("patient-1"), any(Vitals.class));
    }

    @Test
    void ignoresDuplicateEvent() throws Exception {
        VitalEvent event = validEvent();
        when(vitalsRepository.existsByEventId(event.getEventId())).thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(vitalsRepository, never()).save(any(Vitals.class));
        verify(patientRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingPatientAndMissingConsent() throws Exception {
        VitalEvent event = validEvent();
        when(vitalsRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(patientRepository.findById("patient-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(event)))
            .isInstanceOf(IllegalStateException.class);
        verify(consentService, never()).hasActiveConsent(any());

        when(patientRepository.findById("patient-1"))
                .thenReturn(Optional.of(Patient.builder().id("patient-1").build()));
        when(consentService.hasActiveConsent("patient-1")).thenReturn(false);

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(event)))
            .isInstanceOf(IllegalStateException.class);
        verify(vitalsRepository, never()).save(any(Vitals.class));
    }

    @Test
    void rejectsInvalidVitals() throws Exception {
        VitalEvent event = VitalEvent.builder()
            .eventId("invalid-event-1")
            .schemaVersion("1.0")
            .patientId("patient-1")
            .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
            .heartRate(301)
            .systolicBp(120)
            .diastolicBp(80)
            .spo2(98.0)
            .build();
        when(vitalsRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(Patient.builder().id("patient-1").build()));
        when(consentService.hasActiveConsent("patient-1")).thenReturn(true);

        assertThatThrownBy(() -> consumer.consume(objectMapper.writeValueAsString(event)))
            .isInstanceOf(IllegalStateException.class);

        verify(vitalsRepository, never()).save(any(Vitals.class));
    }

    private VitalEvent validEvent() {
        return VitalEvent.builder()
                .eventId("event-1")
                .schemaVersion("1.0")
                .patientId("patient-1")
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .heartRate(72)
                .systolicBp(120)
                .diastolicBp(80)
                .spo2(98.0)
                .build();
    }
}