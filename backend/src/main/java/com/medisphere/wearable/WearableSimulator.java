package com.medisphere.wearable;

import com.medisphere.kafka.event.VitalEvent;
import com.medisphere.kafka.producer.VitalsEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "medisphere.wearable-simulator", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class WearableSimulator {

    private final VitalsEventProducer vitalsEventProducer;

    @org.springframework.beans.factory.annotation.Value("${medisphere.wearable-simulator.patient-id:patient-1}")
    private String patientId;

    @org.springframework.beans.factory.annotation.Value("${medisphere.wearable-simulator.device-id:wearable-dev-1}")
    private String deviceId;

    @org.springframework.beans.factory.annotation.Value("${medisphere.wearable-simulator.profile:NORMAL}")
    private String profile;

    @Scheduled(fixedDelayString = "${medisphere.wearable-simulator.interval-ms:5000}")
    public void emitSyntheticVitals() {
        boolean abnormal = "ABNORMAL".equalsIgnoreCase(profile);
        VitalEvent event = VitalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .schemaVersion("1.0")
                .patientId(patientId)
                .deviceId(deviceId)
                .source("WEARABLE_SIMULATOR")
                .timestamp(Instant.now())
                .heartRate(abnormal ? 135 : 64 + (int) (Math.random() * 14))
                .systolicBp(abnormal ? 155 : 112 + (int) (Math.random() * 10))
                .diastolicBp(abnormal ? 98 : 72 + (int) (Math.random() * 8))
                .spo2(abnormal ? 89.0 : 97.0 + (Math.random() * 1.5))
                .temperature(abnormal ? 39.1 : 36.4 + (Math.random() * 0.8))
                .respiratoryRate(15 + (int) (Math.random() * 4))
                .build();

        vitalsEventProducer.publish(event);
        log.debug("[SIM] Published synthetic vitals event for patient={} device={}", patientId, deviceId);
    }
}