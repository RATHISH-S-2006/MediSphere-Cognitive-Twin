package com.medisphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisphere.domain.FHIRResource;
import com.medisphere.domain.HealthTwin;
import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.repository.HealthTwinRepository;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthTwinServiceTest {

    @Mock
    private HealthTwinRepository healthTwinRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private FHIRResourceRepository fhirResourceRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

        @Mock
        private ObjectMapper objectMapper;

    @InjectMocks
    private HealthTwinService healthTwinService;

    @Test
    void createsAndThenReusesExistingTwin() {
        Patient patient = completePatient();
        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        when(healthTwinRepository.findByPatientId("patient-1")).thenReturn(Optional.empty());
        stubCompleteRelatedData();
        when(healthTwinRepository.save(any(HealthTwin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HealthTwin first = healthTwinService.createOrUpdateTwin("patient-1");
        when(healthTwinRepository.findByPatientId("patient-1")).thenReturn(Optional.of(first));
        HealthTwin second = healthTwinService.createOrUpdateTwin("patient-1");

        assertThat(first.getTwinId()).isNotBlank();
        assertThat(second.getTwinId()).isEqualTo(first.getTwinId());
        assertThat(second.getPatientId()).isEqualTo("patient-1");
        assertThat(second.getCompletenessPercentage()).isEqualTo(90.0);
    }

    @Test
    void calculatesMissingDataPoints() {
        Patient patient = Patient.builder().id("patient-1").build();
        HealthTwin twin = HealthTwin.builder().patientId("patient-1").build();
        when(labResultRepository.findByPatientIdOrderByCollectedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(fhirResourceRepository.findByPatientIdAndResourceType(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(fhirResourceRepository.findByPatientId(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        HealthTwinService.CompletenessResult result = healthTwinService.calculateCompleteness(patient, twin);

        assertThat(result.percentage()).isEqualTo(0.0);
        assertThat(result.missingPoints()).containsExactlyInAnyOrder(
                "demographics.name",
                "demographics.dateOfBirth",
                "demographics.gender",
                "demographics.fhirId",
                "vitals.latestSnapshot",
                "labResults.any",
                "fhirResources.Patient",
                "fhirResources.Observation",
                "fhirResources.sufficient");
    }

    @Test
    void evaluatesAndPersistsCurrentCompleteness() {
        Patient patient = completePatient();
        HealthTwin twin = HealthTwin.builder()
                .twinId("twin-1")
                .patientId("patient-1")
                .build();
        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        when(healthTwinRepository.findByPatientId("patient-1")).thenReturn(Optional.of(twin));
        when(healthTwinRepository.save(any(HealthTwin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubCompleteRelatedData();

        HealthTwinService.CompletenessResult result = healthTwinService.evaluateCompleteness("patient-1");

        assertThat(result.percentage()).isEqualTo(90.0);
        assertThat(twin.getCompletenessPercentage()).isEqualTo(90.0);
        assertThat(twin.getMissingDataPoints()).containsExactly("vitals.latestSnapshot");
    }

    @Test
    void updatesLatestVitalsSnapshot() {
        HealthTwin twin = HealthTwin.builder().twinId("twin-1").patientId("patient-1").build();
        Patient patient = completePatient();
        Vitals vitals = Vitals.builder()
                .patientId("patient-1")
                .heartRate(72)
                .systolicBp(120)
                .diastolicBp(80)
                .spo2(98.0)
                .recordedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .deviceId("device-1")
                .source("WEARABLE_SIMULATOR")
                .build();
        when(healthTwinRepository.findByPatientId("patient-1")).thenReturn(Optional.of(twin));
        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        stubCompleteRelatedData();
        when(healthTwinRepository.save(any(HealthTwin.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        healthTwinService.updateLatestVitals("patient-1", vitals);

        assertThat(twin.getLatestVitals()).isNotNull();
        assertThat(twin.getLatestVitals().getHeartRate()).isEqualTo(72);
        assertThat(twin.getLatestVitals().getSpo2()).isEqualTo(98.0);
        assertThat(twin.getLatestVitals().getDeviceId()).isEqualTo("device-1");
        assertThat(twin.getLastVitalsUpdateAt()).isNotNull();
    }

    private Patient completePatient() {
        return Patient.builder()
                .id("patient-1")
                .fhirPatientId("fhir-patient-1")
                .firstName("Anika")
                .lastName("Shah")
                .dateOfBirth("1986-04-12")
                .gender("female")
                .build();
    }

    private void stubCompleteRelatedData() {
        when(labResultRepository.findByPatientIdOrderByCollectedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(LabResult.builder().id("lab-1").build())));
        when(fhirResourceRepository.findByPatientId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        FHIRResource.builder().id("fhir-1").build(),
                        FHIRResource.builder().id("fhir-2").build(),
                        FHIRResource.builder().id("fhir-3").build())));
        when(fhirResourceRepository.findByPatientIdAndResourceType(any(), any(), any()))
                .thenAnswer(invocation -> {
                    String resourceType = invocation.getArgument(1);
                    return new PageImpl<>(List.of(FHIRResource.builder().resourceType(resourceType).build()));
                });
    }
}