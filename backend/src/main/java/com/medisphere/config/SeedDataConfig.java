package com.medisphere.config;

import com.medisphere.consent.ConsentService;
import com.medisphere.domain.AuditEvent;
import com.medisphere.domain.Consent;
import com.medisphere.domain.FHIRResource;
import com.medisphere.domain.HealthTwin;
import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Provider;
import com.medisphere.domain.Vitals;
import com.medisphere.repository.AuditEventRepository;
import com.medisphere.repository.FHIRResourceRepository;
import com.medisphere.repository.HealthTwinRepository;
import com.medisphere.repository.LabResultRepository;
import com.medisphere.repository.PatientRepository;
import com.medisphere.repository.ProviderRepository;
import com.medisphere.repository.ConsentRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.service.HealthTwinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Development/test-only synthetic data initializer.
 *
 * Seeded data is idempotent and uses only synthetic records. It is intentionally
 * disabled outside dev/test profiles so production deployments cannot be polluted
 * with demo data.
 */
@Configuration
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Slf4j
public class SeedDataConfig {

    @Bean
    public ApplicationRunner seedSyntheticData(
            PatientRepository patientRepository,
            ProviderRepository providerRepository,
            FHIRResourceRepository fhirResourceRepository,
            VitalsRepository vitalsRepository,
            LabResultRepository labResultRepository,
            HealthTwinRepository healthTwinRepository,
            ConsentRepository consentRepository,
            AuditEventRepository auditEventRepository,
            HealthTwinService healthTwinService,
            ConsentService consentService) {
        return args -> {
            seedProviders(providerRepository);
            seedPatients(patientRepository, providerRepository);
            seedFhirResources(fhirResourceRepository, patientRepository);
            seedVitals(vitalsRepository, patientRepository);
            seedLabs(labResultRepository, patientRepository);
            seedConsents(consentRepository, consentService, patientRepository);
            seedTwins(healthTwinService, patientRepository);
            seedAudits(auditEventRepository, patientRepository);

            log.info("[SEED] Synthetic development data initialized");
        };
    }

    private void seedProviders(ProviderRepository providerRepository) {
        List<Provider> providers = List.of(
                provider("provider-1", "1234567890", "Asha", "Menon", "Cardiology", "asha.menon@medisphere.test"),
                provider("provider-2", "1234567891", "Daniel", "Rao", "Internal Medicine", "daniel.rao@medisphere.test")
        );

        for (Provider provider : providers) {
            if (!providerRepository.existsByNpi(provider.getNpi())) {
                providerRepository.save(provider);
            }
        }
    }

    private Provider provider(String id, String npi, String firstName, String lastName,
                              String specialty, String email) {
        return Provider.builder()
                .id(id)
                .npi(npi)
                .firstName(firstName)
                .lastName(lastName)
                .specialty(specialty)
                .email(email)
                .phone("+1-555-0100")
                .organization("MediSphere General Hospital")
                .department(specialty)
                .active(true)
                .schemaVersion("1.0")
                .build();
    }

    private void seedPatients(PatientRepository patientRepository, ProviderRepository providerRepository) {
        List<Patient> patients = List.of(
                patient("patient-1", "fhir-patient-1", "MRN-1001", "Anika", "Shah", "1986-04-12", "female", "anika.shah@medisphere.test"),
                patient("patient-2", "fhir-patient-2", "MRN-1002", "Rahul", "Iyer", "1979-11-03", "male", "rahul.iyer@medisphere.test"),
                patient("patient-3", "fhir-patient-3", "MRN-1003", "Maya", "Nair", "1991-08-27", "female", "maya.nair@medisphere.test")
        );

        for (Patient patient : patients) {
            if (!patientRepository.existsByFhirPatientId(patient.getFhirPatientId())) {
                patient.setProviderIds(providerRepository.findAll().stream().map(Provider::getId).limit(2).toList());
                patientRepository.save(patient);
            }
        }
    }

    private Patient patient(String id, String fhirId, String mrn, String firstName, String lastName,
                            String dob, String gender, String email) {
        return Patient.builder()
                .id(id)
                .fhirPatientId(fhirId)
                .mrn(mrn)
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(dob)
                .gender(gender)
                .email(email)
                .phone("+1-555-01" + mrn.substring(mrn.length() - 2))
                .addressLine1("100 Synthetic Health Ave")
                .city("Bengaluru")
                .state("KA")
                .postalCode("560001")
                .country("IN")
                .active(true)
                .sourceSystem("SYNTHETIC_SEED")
                .providerIds(List.of())
                .schemaVersion("1.0")
                .build();
    }

    private void seedFhirResources(FHIRResourceRepository fhirResourceRepository, PatientRepository patientRepository) {
        patientRepository.findAll().forEach(patient -> {
            List<FHIRResource> resources = List.of(
                    fhirResource(patient.getId(), patient.getFhirPatientId(), "Patient", "fhir-resource-patient-" + patient.getId(), patient.getFhirPatientId()),
                    fhirResource(patient.getId(), "obs-" + patient.getId() + "-1", "Observation", "fhir-resource-obs-1-" + patient.getId(), patient.getFhirPatientId()),
                    fhirResource(patient.getId(), "cond-" + patient.getId(), "Condition", "fhir-resource-cond-" + patient.getId(), patient.getFhirPatientId())
            );

            for (FHIRResource resource : resources) {
                if (!fhirResourceRepository.existsByFhirResourceIdAndResourceType(resource.getFhirResourceId(), resource.getResourceType())) {
                    fhirResourceRepository.save(resource);
                }
            }
        });
    }

    private FHIRResource fhirResource(String patientId, String resourceId, String resourceType,
                                      String recordId, String fhirPatientId) {
        return FHIRResource.builder()
                .id(recordId)
                .fhirResourceId(resourceId)
                .resourceType(resourceType)
                .patientId(patientId)
                .fhirPatientRef("Patient/" + fhirPatientId)
                .sourceSystem("SYNTHETIC_SEED")
                .version("1")
                .validationPassed(true)
                .validationNote("seeded synthetic resource")
                .rawFhirJson("{}")
                .receivedAt(Instant.now())
                .lastUpdated(Instant.now())
                .schemaVersion("1.0")
                .build();
    }

    private void seedVitals(VitalsRepository vitalsRepository, PatientRepository patientRepository) {
        patientRepository.findAll().forEach(patient -> {
            if (vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements() == 0) {
                for (int i = 0; i < 3; i++) {
                    Vitals vitals = Vitals.builder()
                            .id(patient.getId() + "-vitals-" + i)
                            .patientId(patient.getId())
                            .eventId(patient.getId() + "-vital-event-" + i)
                            .heartRate(68 + i)
                            .systolicBp(118 + i)
                            .diastolicBp(76 + i)
                            .spo2(98.0 - i * 0.2)
                            .temperature(36.6)
                            .respiratoryRate(16)
                            .dataQualityValid(true)
                            .dataQualityNote("seeded synthetic vitals")
                            .deviceId("device-" + patient.getId())
                            .source("WEARABLE_SIMULATOR")
                            .recordedAt(Instant.now().minusSeconds(900L * i))
                            .receivedAt(Instant.now())
                            .schemaVersion("1.0")
                            .build();
                    vitalsRepository.save(vitals);
                }
            }
        });
    }

    private void seedLabs(LabResultRepository labResultRepository, PatientRepository patientRepository) {
        patientRepository.findAll().forEach(patient -> {
            if (labResultRepository.findByPatientIdOrderByCollectedAtDesc(patient.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements() == 0) {
                for (int i = 0; i < 2; i++) {
                    LabResult labResult = LabResult.builder()
                            .id(patient.getId() + "-lab-" + i)
                            .patientId(patient.getId())
                            .fhirObservationId("obs-" + patient.getId() + "-" + i)
                            .testName(i == 0 ? "Hemoglobin A1c" : "Lipid Panel")
                            .testCode(i == 0 ? "4548-4" : "24331-1")
                            .value(i == 0 ? "5.7" : "185")
                            .unit(i == 0 ? "%" : "mg/dL")
                            .referenceRange(i == 0 ? "4.0 - 5.6" : "<200")
                            .interpretation(i == 0 ? "HIGH" : "NORMAL")
                            .category("CHEMISTRY")
                            .sourceSystem("SYNTHETIC_SEED")
                            .collectedAt(Instant.now().minusSeconds(7200L * i))
                            .reportedAt(Instant.now())
                            .schemaVersion("1.0")
                            .build();
                    labResultRepository.save(labResult);
                }
            }
        });
    }

    private void seedConsents(ConsentRepository consentRepository, ConsentService consentService, PatientRepository patientRepository) {
        patientRepository.findAll().forEach(patient -> {
            if (consentRepository.findTopByPatientIdAndStatusOrderByGrantedAtDesc(patient.getId(), Consent.ConsentStatus.GRANTED).isEmpty()) {
                consentService.grantConsent(patient.getId(), "TREATMENT", "ALL_DATA", patient.getId(), Instant.now().plusSeconds(60L * 60L * 24L * 180L));
            }
        });
    }

    private void seedTwins(HealthTwinService healthTwinService, PatientRepository patientRepository) {
        patientRepository.findAll().forEach(patient -> healthTwinService.createOrUpdateTwin(patient.getId()));
    }

    private void seedAudits(AuditEventRepository auditEventRepository, PatientRepository patientRepository) {
        Set<String> existingActionKeys = auditEventRepository.findAllByOrderByTimestampDesc(org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(AuditEvent::getAction)
                .collect(java.util.stream.Collectors.toSet());

        patientRepository.findAll().forEach(patient -> {
            if (!existingActionKeys.contains("SEED_PATIENT_" + patient.getId())) {
                auditEventRepository.save(AuditEvent.builder()
                        .auditId(patient.getId() + "-audit-seed")
                        .timestamp(Instant.now())
                        .actorId("seed-system")
                        .actorRole("SYSTEM")
                        .action("SEED_PATIENT_" + patient.getId())
                        .resourceType("Patient")
                        .resourceId(patient.getId())
                        .patientId(patient.getId())
                        .outcome("SUCCESS")
                        .outcomeDetail("Synthetic patient seed record created")
                        .correlationId("seed-correlation")
                        .requestPath("/seed/dev")
                        .clientIp("127.0.0.1")
                        .schemaVersion("1.0")
                        .build());
            }
        });
    }
}