package com.medisphere.fhir;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.medisphere.fhir.validator.FhirResourceValidator;
import com.medisphere.fhir.validator.FhirValidationReport;

import ca.uhn.fhir.context.FhirContext;

class FhirResourceValidatorTest {

    private FhirResourceValidator validator;
    private FhirContext fhirContext;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4();
        validator = new FhirResourceValidator(fhirContext);
    }

    @Test
    void acceptsValidPatientResource() {
        String patient = """
                {"resourceType":"Patient","id":"patient-1","name":[{"family":"Shah","given":["Anika"]}],"birthDate":"1986-04-12"}
                """;

        FhirValidationReport report = validator.validateAndReport(patient);

        assertThat(report.isValid()).isTrue();
        assertThat(report.getResourceType()).isEqualTo("Patient");
        assertThat(report.getResourceId()).isEqualTo("patient-1");
        assertThat(report.getErrors()).isEmpty();
    }

    @Test
    void rejectsInvalidObservationResource() {
        String observation = """
                {"resourceType":"Observation","id":"observation-1"}
                """;

        FhirValidationReport report = validator.validateAndReport(observation);

        assertThat(report.isValid()).isFalse();
        assertThat(report.getErrors()).contains(
                "Observation must have a subject (patient reference)",
                "Observation must have a status",
                "Observation must have a code",
                "Observation must have an effective date or issued timestamp");
    }

    @Test
    void rejectsEmptyAndMalformedJson() {
        assertThat(validator.validateAndReport(" ").getErrors())
                .containsExactly("Resource JSON is empty");
        assertThat(validator.validateAndReport("not-json").getErrors().getFirst())
                .startsWith("Invalid FHIR JSON:");
    }
}