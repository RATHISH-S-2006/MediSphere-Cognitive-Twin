package com.medisphere.risk;

import com.medisphere.domain.Patient;
import com.medisphere.domain.LabResult;
import com.medisphere.domain.Vitals;
import com.medisphere.repository.PatientRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.RiskPredictionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskPredictionServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private VitalsRepository vitalsRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private RiskPredictionRepository riskPredictionRepository;

        @Mock
        private MlPredictionClient mlPredictionClient;

    @InjectMocks
    private RiskPredictionService riskPredictionService;

    @Test
    void createsPredictionResponseFromPatientContext() {
        Patient patient = Patient.builder()
                .id("patient-1")
                .fhirPatientId("fhir-patient-1")
                .firstName("Anika")
                .lastName("Shah")
                .dateOfBirth("1986-04-12")
                .gender("female")
                .build();

        when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
        when(vitalsRepository.findTopByPatientIdOrderByRecordedAtDesc("patient-1"))
                .thenReturn(Optional.of(Vitals.builder()
                        .patientId("patient-1")
                        .heartRate(76)
                        .systolicBp(138)
                        .diastolicBp(88)
                        .spo2(98.0)
                        .recordedAt(Instant.now())
                        .build()));
        when(labResultRepository.findByPatientIdOrderByCollectedAtDesc(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        when(riskPredictionRepository.save(any(RiskPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mlPredictionClient.predict(any(), any(), any()))
                .thenReturn(new MlPredictionClient.MlPredictionResponse(
                        "patient-1", "CARDIOVASCULAR", 0.61, "MODERATE", "cardiovascular-model-v1",
                        "2026-09-15T00:00:00Z",
                        List.of(new MlPredictionClient.MlExplanation(
                                "systolicBloodPressure", 138.0, 0.25, "INCREASES_RISK"))));

        RiskPredictionResponse response = riskPredictionService.buildAndPersistPrediction(
                "patient-1",
                RiskPredictionRequest.forCardiovascular("patient-1")
        );

        assertThat(response).isNotNull();
        assertThat(response.patientId()).isEqualTo("patient-1");
        assertThat(response.modelType()).isEqualTo("CARDIOVASCULAR");
        assertThat(response.riskCategory()).isNotBlank();
    }

        @Test
        void mapsGlucoseAndHba1cFromTheirOwnLabs() {
                Patient patient = Patient.builder().id("patient-1").gender("female").dateOfBirth("1986-04-12").build();
                Vitals vitals = Vitals.builder().systolicBp(120).diastolicBp(80).heartRate(70).build();
                List<LabResult> labs = List.of(
                                LabResult.builder().testName("Hemoglobin A1c").testCode("4548-4").value("6.4").build(),
                                LabResult.builder().testName("Glucose").testCode("2339-0").value("118").build());

                RiskPredictionService.FeatureSummary summary = riskPredictionService.extractFeatureSummary(patient, vitals, labs);

                assertThat(summary.glucose()).isEqualTo(118.0);
                assertThat(summary.hba1c()).isEqualTo(6.4);
        }
}
