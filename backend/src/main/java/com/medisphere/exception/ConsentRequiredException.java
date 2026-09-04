package com.medisphere.exception;

public class ConsentRequiredException extends RuntimeException {
    private final String patientId;
    private final String reason;

    public ConsentRequiredException(String patientId) {
        super("Patient consent is required to access data for patient: " + patientId);
        this.patientId = patientId;
        this.reason = "No active consent found";
    }

    public ConsentRequiredException(String patientId, String reason) {
        super("Patient consent is " + reason + " for patient: " + patientId);
        this.patientId = patientId;
        this.reason = reason;
    }

    public String getPatientId() { return patientId; }
    public String getReason() { return reason; }
}
