package com.medisphere.fhir;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.fhir.mapper.FhirToDomainMapper;

class FhirToDomainMapperTest {

    private final FhirToDomainMapper mapper = new FhirToDomainMapper();

    @Test
    void mapsPatientDemographics() {
                org.hl7.fhir.r4.model.Patient source = new org.hl7.fhir.r4.model.Patient();
                source.setId("patient-1");
                source.addName(new HumanName().setFamily("Shah").addGiven("Anika"));
                source.setGender(AdministrativeGender.FEMALE);
                source.setBirthDateElement(new DateType("1986-04-12"));
                source.setActiveElement(new BooleanType(true));

        Patient result = mapper.mapPatient(source);

        assertThat(result.getId()).isNotBlank();
        assertThat(result.getFhirPatientId()).isEqualTo("patient-1");
        assertThat(result.getFirstName()).isEqualTo("Anika");
        assertThat(result.getLastName()).isEqualTo("Shah");
        assertThat(result.getGender()).isEqualTo("female");
        assertThat(result.getDateOfBirth()).isEqualTo("1986-04-12");
        assertThat(result.getSourceSystem()).isEqualTo("FHIR");
    }

    @Test
    void mapsObservationQuantityAndReferenceRange() {
        Date effective = new Date(1_700_000_000_000L);
        Observation source = new Observation();
        source.setId("observation-1");
        source.setCode(new org.hl7.fhir.r4.model.CodeableConcept()
                .addCoding(new Coding("loinc", "4548-4", "Hemoglobin A1c")));
        source.setValue(new Quantity().setValue(5.7).setUnit("%"));
        source.setEffective(new DateTimeType(effective));
        source.addReferenceRange()
                .setLow(new Quantity().setValue(4.0))
                .setHigh(new Quantity().setValue(5.6));

        LabResult result = mapper.mapObservationToLabResult(source, "patient-1");

        assertThat(result.getPatientId()).isEqualTo("patient-1");
        assertThat(result.getFhirObservationId()).isEqualTo("observation-1");
        assertThat(result.getTestName()).isEqualTo("Hemoglobin A1c");
        assertThat(result.getTestCode()).isEqualTo("4548-4");
        assertThat(result.getValue()).isEqualTo("5.7");
        assertThat(result.getUnit()).isEqualTo("%");
        assertThat(result.getReferenceRange()).isEqualTo("4.0 - 5.6");
        assertThat(result.getCollectedAt()).isEqualTo(effective.toInstant());
    }
}