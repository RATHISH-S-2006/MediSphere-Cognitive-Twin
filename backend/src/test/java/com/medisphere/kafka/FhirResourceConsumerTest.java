package com.medisphere.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medisphere.audit.AuditService;
import com.medisphere.domain.FHIRResource;
import com.medisphere.kafka.consumer.FhirResourceConsumer;
import com.medisphere.kafka.event.FhirResourceEvent;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.service.HealthTwinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FhirResourceConsumerTest {

    @Mock private FHIRResourceRepository fhirResourceRepository;
    @Mock private HealthTwinService healthTwinService;
    @Mock private AuditService auditService;

    private FhirResourceConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new FhirResourceConsumer(objectMapper, fhirResourceRepository, healthTwinService, auditService);
    }

    @Test
    void persistsResourceAndSynchronizesTwin() throws Exception {
        FhirResourceEvent event = validEvent();
        when(fhirResourceRepository.existsByFhirResourceIdAndResourceType("fhir-1", "Patient"))
                .thenReturn(false);

        consumer.consume(objectMapper.writeValueAsString(event));

        verify(fhirResourceRepository).save(any(FHIRResource.class));
        verify(healthTwinService).createOrUpdateTwin("patient-1");
    }

    @Test
    void ignoresDuplicateResource() throws Exception {
        when(fhirResourceRepository.existsByFhirResourceIdAndResourceType("fhir-1", "Patient"))
                .thenReturn(true);

        consumer.consume(objectMapper.writeValueAsString(validEvent()));

        verify(fhirResourceRepository, never()).save(any(FHIRResource.class));
        verify(healthTwinService, never()).createOrUpdateTwin(any());
    }

    @Test
    void invalidEventIsNotSilentlyAccepted() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.consume("not-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FHIR event processing failed");
    }

    private FhirResourceEvent validEvent() {
        return FhirResourceEvent.builder()
                .eventId("event-1")
                .schemaVersion("1.0")
                .patientId("patient-1")
                .fhirResourceId("fhir-1")
                .resourceType("Patient")
                .sourceSystem("HAPI_FHIR")
                .timestamp(Instant.parse("2026-01-01T00:00:00Z"))
                .rawFhirJson("{\"resourceType\":\"Patient\"}")
                .build();
    }
}