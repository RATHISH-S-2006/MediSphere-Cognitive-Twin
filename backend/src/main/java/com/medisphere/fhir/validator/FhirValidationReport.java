package com.medisphere.fhir.validator;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Structured FHIR validation result returned by FhirResourceValidator.
 */
@Data
@Builder
public class FhirValidationReport {

    private boolean valid;
    private String resourceType;
    private String resourceId;
    private List<String> errors;

    public static FhirValidationReport valid(String resourceType, String resourceId) {
        return FhirValidationReport.builder()
                .valid(true)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .errors(List.of())
                .build();
    }

    public static FhirValidationReport invalid(String resourceType, String resourceId, List<String> errors) {
        return FhirValidationReport.builder()
                .valid(false)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .errors(errors)
                .build();
    }
}
