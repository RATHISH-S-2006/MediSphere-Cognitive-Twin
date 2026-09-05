package com.medisphere.mapper;

import com.medisphere.domain.AuditEvent;
import com.medisphere.domain.Consent;
import com.medisphere.domain.FHIRResource;
import com.medisphere.domain.HealthTwin;
import com.medisphere.domain.LabResult;
import com.medisphere.domain.Patient;
import com.medisphere.domain.Vitals;
import com.medisphere.dto.AuditDtos;
import com.medisphere.dto.ConsentDtos;
import com.medisphere.dto.FhirDtos;
import com.medisphere.dto.HealthTwinDtos;
import com.medisphere.dto.LabResultDtos;
import com.medisphere.dto.PatientDtos;
import com.medisphere.dto.VitalsDtos;
import com.medisphere.fhir.validator.FhirValidationReport;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ApiDtoMapper {

    public PatientDtos.PatientSummaryResponse toPatientSummary(Patient patient) {
        return new PatientDtos.PatientSummaryResponse(
                patient.getId(),
                patient.getFhirPatientId(),
                patient.getMrn(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.isActive(),
                patient.getSourceSystem()
        );
    }

    public PatientDtos.PatientDetailResponse toPatientDetail(Patient patient) {
        return new PatientDtos.PatientDetailResponse(
                patient.getId(),
                patient.getFhirPatientId(),
                patient.getMrn(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getAddressLine1(),
                patient.getAddressLine2(),
                patient.getCity(),
                patient.getState(),
                patient.getPostalCode(),
                patient.getCountry(),
                patient.isActive(),
                patient.getSourceSystem(),
                safeList(patient.getProviderIds()),
                patient.getSchemaVersion(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }

    public Patient toPatientEntity(PatientDtos.PatientCreateRequest request) {
        Patient patient = new Patient();
        patient.setFhirPatientId(request.fhirPatientId());
        patient.setMrn(request.mrn());
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setEmail(request.email());
        patient.setPhone(request.phone());
        patient.setAddressLine1(request.addressLine1());
        patient.setAddressLine2(request.addressLine2());
        patient.setCity(request.city());
        patient.setState(request.state());
        patient.setPostalCode(request.postalCode());
        patient.setCountry(request.country());
        patient.setActive(request.active() == null || request.active());
        patient.setSourceSystem(request.sourceSystem());
        patient.setProviderIds(safeList(request.providerIds()));
        return patient;
    }

    public void applyPatientUpdate(Patient patient, PatientDtos.PatientUpdateRequest request) {
        if (request.mrn() != null) patient.setMrn(request.mrn());
        if (request.firstName() != null) patient.setFirstName(request.firstName());
        if (request.lastName() != null) patient.setLastName(request.lastName());
        if (request.dateOfBirth() != null) patient.setDateOfBirth(request.dateOfBirth());
        if (request.gender() != null) patient.setGender(request.gender());
        if (request.email() != null) patient.setEmail(request.email());
        if (request.phone() != null) patient.setPhone(request.phone());
        if (request.addressLine1() != null) patient.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null) patient.setAddressLine2(request.addressLine2());
        if (request.city() != null) patient.setCity(request.city());
        if (request.state() != null) patient.setState(request.state());
        if (request.postalCode() != null) patient.setPostalCode(request.postalCode());
        if (request.country() != null) patient.setCountry(request.country());
        if (request.active() != null) patient.setActive(request.active());
        if (request.sourceSystem() != null) patient.setSourceSystem(request.sourceSystem());
        if (request.providerIds() != null) patient.setProviderIds(safeList(request.providerIds()));
    }

    public HealthTwinDtos.LatestVitalsSnapshotDto toVitalsSnapshot(HealthTwin.VitalsSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new HealthTwinDtos.LatestVitalsSnapshotDto(
                snapshot.getHeartRate(),
                snapshot.getSystolicBp(),
                snapshot.getDiastolicBp(),
                snapshot.getSpo2(),
                snapshot.getRecordedAt(),
                snapshot.getDeviceId(),
                snapshot.getSource()
        );
    }

    public HealthTwinDtos.HealthTwinResponse toHealthTwinResponse(HealthTwin twin) {
        return new HealthTwinDtos.HealthTwinResponse(
                twin.getTwinId(),
                twin.getPatientId(),
                twin.getModelVersion(),
                twin.getPatientFirstName(),
                twin.getPatientLastName(),
                twin.getPatientDateOfBirth(),
                twin.getPatientGender(),
                toVitalsSnapshot(twin.getLatestVitals()),
                safeList(twin.getRecentLabResultIds()),
                safeList(twin.getFhirResourceIds()),
                twin.getActiveConsentId(),
                twin.getConsentStatus(),
                twin.getCompletenessPercentage(),
                safeList(twin.getMissingDataPoints()),
                twin.getLastFhirSyncAt(),
                twin.getLastVitalsUpdateAt(),
                twin.getSchemaVersion(),
                twin.getCreatedAt(),
                twin.getUpdatedAt()
        );
    }

    public HealthTwinDtos.TwinCompletenessResponse toCompletenessResponse(HealthTwin twin) {
        return new HealthTwinDtos.TwinCompletenessResponse(
                twin.getPatientId(),
                twin.getTwinId(),
                twin.getCompletenessPercentage(),
                safeList(twin.getMissingDataPoints()),
                Instant.now()
        );
    }

    public HealthTwinDtos.TwinSyncResponse toTwinSyncResponse(HealthTwin twin) {
        return new HealthTwinDtos.TwinSyncResponse(
                twin.getPatientId(),
                twin.getTwinId(),
                twin.getCompletenessPercentage(),
                safeList(twin.getMissingDataPoints()),
                Instant.now()
        );
    }

    public VitalsDtos.VitalsResponse toVitalsResponse(Vitals vitals) {
        return new VitalsDtos.VitalsResponse(
                vitals.getId(),
                vitals.getPatientId(),
                vitals.getEventId(),
                vitals.getHeartRate(),
                vitals.getSystolicBp(),
                vitals.getDiastolicBp(),
                vitals.getSpo2(),
                vitals.getTemperature(),
                vitals.getRespiratoryRate(),
                vitals.isDataQualityValid(),
                vitals.getDataQualityNote(),
                vitals.getDeviceId(),
                vitals.getSource(),
                vitals.getRecordedAt(),
                vitals.getReceivedAt(),
                vitals.getSchemaVersion(),
                vitals.getCreatedAt()
        );
    }

    public Vitals toVitalsEntity(VitalsDtos.VitalsCreateRequest request) {
        return Vitals.builder()
                .patientId(request.patientId())
                .eventId(request.eventId())
                .heartRate(request.heartRate())
                .systolicBp(request.systolicBp())
                .diastolicBp(request.diastolicBp())
                .spo2(request.spo2())
                .temperature(request.temperature())
                .respiratoryRate(request.respiratoryRate())
                .deviceId(request.deviceId())
                .source(request.source())
                .recordedAt(request.recordedAt())
                .receivedAt(Instant.now())
                .schemaVersion("1.0")
                .build();
    }

    public LabResultDtos.LabResultResponse toLabResultResponse(LabResult labResult) {
        return new LabResultDtos.LabResultResponse(
                labResult.getId(),
                labResult.getPatientId(),
                labResult.getFhirObservationId(),
                labResult.getTestName(),
                labResult.getTestCode(),
                labResult.getValue(),
                labResult.getUnit(),
                labResult.getReferenceRange(),
                labResult.getInterpretation(),
                labResult.getCategory(),
                labResult.getSourceSystem(),
                labResult.getCollectedAt(),
                labResult.getReportedAt(),
                labResult.getSchemaVersion(),
                labResult.getCreatedAt()
        );
    }

    public AuditDtos.AuditEventResponse toAuditEventResponse(AuditEvent auditEvent) {
        return new AuditDtos.AuditEventResponse(
                auditEvent.getAuditId(),
                auditEvent.getTimestamp(),
                auditEvent.getActorId(),
                auditEvent.getActorRole(),
                auditEvent.getAction(),
                auditEvent.getResourceType(),
                auditEvent.getResourceId(),
                auditEvent.getPatientId(),
                auditEvent.getOutcome(),
                auditEvent.getOutcomeDetail(),
                auditEvent.getCorrelationId(),
                auditEvent.getRequestPath(),
                auditEvent.getClientIp(),
                auditEvent.getSchemaVersion()
        );
    }

    public ConsentDtos.ConsentResponse toConsentResponse(Consent consent) {
        return new ConsentDtos.ConsentResponse(
                consent.getConsentId(),
                consent.getPatientId(),
                consent.getStatus() == null ? null : consent.getStatus().name(),
                consent.getPurpose(),
                consent.getScope(),
                consent.getGrantedBy(),
                consent.getGrantedAt(),
                consent.getExpiresAt(),
                consent.getRevokedAt(),
                consent.getRevokedBy(),
                consent.getRevokeReason(),
                consent.getNotes(),
                consent.getSchemaVersion(),
                consent.getCreatedAt(),
                consent.getUpdatedAt()
        );
    }

    public FhirDtos.FhirValidationResponse toFhirValidationResponse(FhirValidationReport report) {
        return new FhirDtos.FhirValidationResponse(
                report.isValid(),
                report.getResourceType(),
                report.getResourceId(),
                safeList(report.getErrors())
        );
    }

    public FhirDtos.FhirResourceResponse toFhirResourceResponse(FHIRResource resource) {
        return new FhirDtos.FhirResourceResponse(
                resource.getId(), resource.getFhirResourceId(), resource.getResourceType(),
                resource.getPatientId(), resource.getFhirPatientRef(), resource.getSourceSystem(),
                resource.getVersion(), resource.isValidationPassed(), resource.getLastUpdated());
    }

    public FhirDtos.FhirSyncResponse toFhirSyncResponse(String patientId, String resourceType, String resourceId,
                                                        boolean persisted, boolean published, String message) {
        return new FhirDtos.FhirSyncResponse(patientId, resourceType, resourceId, persisted, published, message);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }
}