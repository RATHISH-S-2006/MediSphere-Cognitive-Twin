package com.medisphere.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces patient-level authorization independently from role checks.
 * Providers and administrators may access assigned patient routes; a patient
 * token may access only the patient represented by its subject.
 */
@Component("patientAccessChecker")
public class PatientAccessChecker {

    public boolean canAccess(String patientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean elevated = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROVIDER")
                        || authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_CLINICIAN"));
        return elevated || (hasPatientRole(authentication) && patientId.equals(authentication.getName()));
    }

    private boolean hasPatientRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PATIENT"));
    }
}