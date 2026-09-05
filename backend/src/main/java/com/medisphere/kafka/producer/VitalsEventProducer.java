package com.medisphere.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.kafka.event.KafkaTopics;
import com.medisphere.kafka.event.VitalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VitalsEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(VitalEvent event) {
        try {
            kafkaTemplate.send(KafkaTopics.VITALS, event.getPatientId(), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("[KAFKA] Failed to publish vitals event {}: {}", event.getEventId(), ex.getMessage());
        }
    }
}