package com.medisphere.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.kafka.event.FhirResourceEvent;
import com.medisphere.kafka.event.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(FhirResourceEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.FHIR_RESOURCES, event.getPatientId(), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed to publish FHIR event {}: {}", event.getEventId(), ex.getMessage());
        }
    }
}