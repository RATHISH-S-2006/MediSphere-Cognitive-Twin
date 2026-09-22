package com.medisphere.controller;

import com.medisphere.careplan.CarePlan;
import com.medisphere.careplan.CarePlanService;
import com.medisphere.consent.ConsentService;
import com.medisphere.security.PatientAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanControllerTest {
    @Mock private CarePlanService service;
    @Mock private PatientAccessChecker accessChecker;
    @Mock private ConsentService consentService;

    @Test
    void rejectsUnknownAdherenceAction() {
        CarePlan plan = CarePlan.builder().id("plan-1").patientId("patient-1").build();
        when(service.get("plan-1")).thenReturn(plan);
        when(accessChecker.canAccess("patient-1")).thenReturn(true);

        CarePlanController controller = new CarePlanController(service, accessChecker, consentService);

        assertThatThrownBy(() -> controller.adherence("plan-1", "intervention-1", "unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown adherence action");
        verify(service, never()).markAdherence(anyString(), anyString(), any(), any());
        verify(consentService).verifyConsent("patient-1");
    }
}