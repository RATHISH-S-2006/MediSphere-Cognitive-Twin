package com.medisphere.monitoring;

public class InvalidAlertStateException extends RuntimeException {
    public InvalidAlertStateException(String message) {
        super(message);
    }
}
