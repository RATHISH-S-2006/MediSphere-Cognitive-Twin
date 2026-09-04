package com.medisphere.fhir.mapper;

import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Maps FHIR R4 resources to internal domain models.
 * Domain models are decoupled from FHIR resource structure.
 * This mapper is the only class that knows about both FHIR and internal models.
 */
@Component
public class FhirToDomainMapper {

    public Patient mapPatient(org.hl7.fhir.r4.model.Patient fhirPatient) {
        HumanName name = fhirPatient.getNameFirstRep();
        String firstName = name.getGivenAsSingleString();
        String lastName = name.getFamily();

        String phone = fhirPatient.getTelecomFirstRep() != null ?
                fhirPatient.getTelecomFirstRep().getValue() : null;

        String gender = fhirPatient.getGender() != null ?
                fhirPatient.getGender().toCode() : null;

        String dob = fhirPatient.getBirthDateElement() != null ?
                fhirPatient.getBirthDateElement().getValueAsString() : null;

        return Patient.builder()
                .id(UUID.randomUUID().toString())
                .fhirPatientId(fhirPatient.getIdElement().getIdPart())
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(dob)
                .gender(gender)
                .phone(phone)
                .active(fhirPatient.getActive())
                .sourceSystem("FHIR")
                .schemaVersion("1.0")
                .build();
    }

    public LabResult mapObservationToLabResult(Observation obs, String internalPatientId) {
        String value = null;
        String unit = null;
        String refRange = null;

        if (obs.hasValueQuantity()) {
            Quantity q = obs.getValueQuantity();
            value = q.getValue() != null ? q.getValue().toPlainString() : null;
            unit = q.getUnit();
        } else if (obs.hasValueStringType()) {
            value = obs.getValueStringType().getValue();
        }

        if (!obs.getReferenceRange().isEmpty()) {
            var rr = obs.getReferenceRangeFirstRep();
            StringBuilder sb = new StringBuilder();
            if (rr.getLow() != null && rr.getLow().getValue() != null)
                sb.append(rr.getLow().getValue().toPlainString());
            sb.append(" - ");
            if (rr.getHigh() != null && rr.getHigh().getValue() != null)
                sb.append(rr.getHigh().getValue().toPlainString());
            refRange = sb.toString().trim();
        }

        Instant collected = null;
        if (obs.hasEffectiveDateTimeType()) {
            Date d = obs.getEffectiveDateTimeType().getValue();
            if (d != null) collected = d.toInstant();
        }

        String testName = obs.getCode().getCodingFirstRep().getDisplay();
        String testCode = obs.getCode().getCodingFirstRep().getCode();
        if (testName == null && obs.getCode().hasText()) {
            testName = obs.getCode().getText();
        }

        return LabResult.builder()
                .id(UUID.randomUUID().toString())
                .patientId(internalPatientId)
                .fhirObservationId(obs.getIdElement().getIdPart())
                .testName(testName)
                .testCode(testCode)
                .value(value)
                .unit(unit)
                .referenceRange(refRange)
                .sourceSystem("FHIR")
                .collectedAt(collected)
                .reportedAt(Instant.now())
                .schemaVersion("1.0")
                .build();
    }
}
