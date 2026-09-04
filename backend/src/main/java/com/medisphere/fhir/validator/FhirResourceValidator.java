package com.medisphere.fhir.validator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * FHIR R4 Resource Validator.
 * Validates resources before they enter the internal patient twin.
 * Invalid resources are rejected with meaningful error messages.
 *
 * M1 validates: type, ID, required fields, patient references, observation values, timestamps.
 * M2/M3/M4 may extend this with additional clinical validation rules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FhirResourceValidator {

    private final FhirContext fhirContext;

    public ValidationResult validate(String resourceType, String resourceJson) {
        var resource = fhirContext.newJsonParser().parseResource(resourceJson);
        FhirValidator validator = fhirContext.newValidator();
        return validator.validateWithResult(resource);
    }

    public FhirValidationReport validateAndReport(String resourceJson) {
        List<String> errors = new ArrayList<>();

        if (resourceJson == null || resourceJson.isBlank()) {
            return FhirValidationReport.invalid(null, null, List.of("Resource JSON is empty"));
        }

        Resource resource;
        try {
            resource = (Resource) fhirContext.newJsonParser().parseResource(resourceJson);
        } catch (Exception e) {
            return FhirValidationReport.invalid(null, null, List.of("Invalid FHIR JSON: " + e.getMessage()));
        }

        String resourceType = resource.getResourceType().name();
        String resourceId = resource.getIdElement().getIdPart();

        // Validate by resource type
        switch (resourceType) {
            case "Patient" -> validatePatient((Patient) resource, errors);
            case "Observation" -> validateObservation((Observation) resource, errors);
            case "DiagnosticReport" -> validateDiagnosticReport((DiagnosticReport) resource, errors);
            case "Condition" -> validateCondition((Condition) resource, errors);
            case "MedicationRequest" -> validateMedicationRequest((MedicationRequest) resource, errors);
            default -> errors.add("Unsupported resource type: " + resourceType + ". Supported: Patient, Observation, DiagnosticReport, Condition, MedicationRequest");
        }

        if (errors.isEmpty()) {
            return FhirValidationReport.valid(resourceType, resourceId);
        }
        return FhirValidationReport.invalid(resourceType, resourceId, errors);
    }

    private void validatePatient(Patient patient, List<String> errors) {
        if (patient.getIdElement() == null || patient.getIdElement().isEmpty()) {
            errors.add("Patient resource must have an ID");
        }
        if (patient.getName() == null || patient.getName().isEmpty()) {
            errors.add("Patient must have at least one name");
        }
        if (patient.getBirthDateElement() == null || patient.getBirthDateElement().isEmpty()) {
            errors.add("Patient must have a birth date");
        }
    }

    private void validateObservation(Observation obs, List<String> errors) {
        if (obs.getSubject() == null || obs.getSubject().isEmpty()) {
            errors.add("Observation must have a subject (patient reference)");
        }
        if (obs.getStatus() == null) {
            errors.add("Observation must have a status");
        }
        if (obs.getCode() == null || obs.getCode().isEmpty()) {
            errors.add("Observation must have a code");
        }
        if (obs.getEffective() == null && obs.getIssued() == null) {
            errors.add("Observation must have an effective date or issued timestamp");
        }
        // Validate numeric value if present
        if (obs.hasValueQuantity()) {
            Quantity q = obs.getValueQuantity();
            if (q.getValue() == null) {
                errors.add("Observation value quantity must have a value");
            }
        }
    }

    private void validateDiagnosticReport(DiagnosticReport report, List<String> errors) {
        if (report.getSubject() == null || report.getSubject().isEmpty()) {
            errors.add("DiagnosticReport must have a subject");
        }
        if (report.getStatus() == null) {
            errors.add("DiagnosticReport must have a status");
        }
        if (report.getCode() == null || report.getCode().isEmpty()) {
            errors.add("DiagnosticReport must have a code");
        }
    }

    private void validateCondition(Condition condition, List<String> errors) {
        if (condition.getSubject() == null || condition.getSubject().isEmpty()) {
            errors.add("Condition must have a subject");
        }
        if (condition.getCode() == null || condition.getCode().isEmpty()) {
            errors.add("Condition must have a code");
        }
    }

    private void validateMedicationRequest(MedicationRequest medReq, List<String> errors) {
        if (medReq.getSubject() == null || medReq.getSubject().isEmpty()) {
            errors.add("MedicationRequest must have a subject");
        }
        if (medReq.getStatus() == null) {
            errors.add("MedicationRequest must have a status");
        }
    }
}
