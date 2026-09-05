package com.medisphere.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientAccessCheckerTest {

    private final PatientAccessChecker checker = new PatientAccessChecker();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patientCanAccessOnlyOwnPatient() {
        authenticate("patient-1", "ROLE_PATIENT");

        assertThat(checker.canAccess("patient-1")).isTrue();
        assertThat(checker.canAccess("patient-2")).isFalse();
    }

    @Test
    void providerAndAdminCanAccessPatientRoutes() {
        authenticate("provider-1", "ROLE_PROVIDER");
        assertThat(checker.canAccess("patient-2")).isTrue();

        authenticate("admin-1", "ROLE_ADMIN");
        assertThat(checker.canAccess("patient-2")).isTrue();
    }

    @Test
    void anonymousAuthenticationCannotAccessPatientRoutes() {
        assertThat(checker.canAccess("patient-1")).isFalse();
    }

    private void authenticate(String subject, String role) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(
                subject, "N/A", List.of(new SimpleGrantedAuthority(role)));
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}