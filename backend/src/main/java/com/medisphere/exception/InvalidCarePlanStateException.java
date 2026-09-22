package com.medisphere.exception;

public class InvalidCarePlanStateException extends RuntimeException {
    public InvalidCarePlanStateException(String message) {
        super(message);
    }
}