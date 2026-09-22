package com.medisphere.careplan;

import com.medisphere.audit.AuditService;
import com.medisphere.exception.InvalidCarePlanStateException;
import com.medisphere.monitoring.Alert;
import com.medisphere.monitoring.AlertSeverity;
import com.medisphere.monitoring.AlertStatus;
import com.medisphere.repository.AdherenceRecordRepository;
import com.medisphere.repository.AlertRepository;
import com.medisphere.repository.CarePlanRepository;
import com.medisphere.repository.OutcomeRepository;
import com.medisphere.repository.RiskPredictionRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.risk.RiskPrediction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {
    @Mock private CarePlanRepository carePlanRepository;
    @Mock private AdherenceRecordRepository adherenceRepository;
    @Mock private OutcomeRepository outcomeRepository;
    @Mock private RiskPredictionRepository riskRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private VitalsRepository vitalsRepository;
    @Mock private AuditService auditService;

    @Test
    void generatesPlanWithRiskAndAlertTraceability() {
        when(carePlanRepository.findFirstByPatientIdAndStatusOrderByUpdatedAtDesc("patient-1", CarePlanStatus.ACTIVE)).thenReturn(Optional.empty());
        when(riskRepository.findByPatientIdOrderByGeneratedAtDesc(eq("patient-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(RiskPrediction.builder().id("risk-1").patientId("patient-1").modelType("CARDIOVASCULAR").build())));
        when(alertRepository.findByPatientIdAndStatusInOrderByCreatedAtDesc(eq("patient-1"), anyList(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(Alert.builder().id("alert-1").patientId("patient-1").alertType("HEART_RATE_HIGH").severity(AlertSeverity.HIGH).status(AlertStatus.ACTIVE).build())));
        when(vitalsRepository.findTopByPatientIdOrderByRecordedAtDesc("patient-1")).thenReturn(Optional.empty());
        when(carePlanRepository.save(any(CarePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarePlan plan = service().generate("patient-1");

        assertThat(plan.getBasedOnRiskPredictionIds()).containsExactly("risk-1");
        assertThat(plan.getBasedOnAlertIds()).containsExactly("alert-1");
        assertThat(plan.getGenerationReasons()).anyMatch(reason -> reason.contains("cardiovascular"));
        assertThat(plan.getInterventions()).isNotEmpty();
    }

    @Test
    void reusesExistingActivePlan() {
        CarePlan active = CarePlan.builder().id("plan-1").patientId("patient-1").status(CarePlanStatus.ACTIVE).build();
        when(carePlanRepository.findFirstByPatientIdAndStatusOrderByUpdatedAtDesc("patient-1", CarePlanStatus.ACTIVE)).thenReturn(Optional.of(active));

        assertThat(service().generate("patient-1")).isSameAs(active);
        verifyNoInteractions(riskRepository, alertRepository, vitalsRepository);
    }

    @Test
    void rejectsInvalidLifecycleTransition() {
        CarePlan completed = CarePlan.builder().id("plan-1").patientId("patient-1").status(CarePlanStatus.COMPLETED).build();
        when(carePlanRepository.findById("plan-1")).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service().transition("plan-1", CarePlanStatus.ACTIVE))
                .isInstanceOf(InvalidCarePlanStateException.class);
    }

    @Test
    void calculatesAdherenceFromCompletedAndMissedRecords() {
        CarePlan plan = CarePlan.builder().id("plan-1").patientId("patient-1")
                .status(CarePlanStatus.ACTIVE)
                .interventions(List.of(Intervention.builder().id("int-1").status(InterventionStatus.PENDING).build()))
                .build();
        when(carePlanRepository.findById("plan-1")).thenReturn(Optional.of(plan));
        when(adherenceRepository.findByInterventionIdAndDate(eq("int-1"), any())).thenReturn(Optional.empty());
        when(adherenceRepository.findByCarePlanId("plan-1")).thenReturn(List.of(
                AdherenceRecord.builder().status(AdherenceStatus.COMPLETED).build(),
                AdherenceRecord.builder().status(AdherenceStatus.MISSED).build()));
        when(adherenceRepository.save(any(AdherenceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(carePlanRepository.save(any(CarePlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CarePlan updated = service().markAdherence("plan-1", "int-1", AdherenceStatus.COMPLETED, null);

        assertThat(updated.getAdherenceSummary().getCompleted()).isEqualTo(1);
        assertThat(updated.getAdherenceSummary().getMissed()).isEqualTo(1);
        assertThat(updated.getAdherenceSummary().getAdherencePercentage()).isEqualTo(50.0);
    }

    @Test
    void rejectsAdherenceForCompletedPlan() {
        rejectsAdherenceForTerminalPlan(CarePlanStatus.COMPLETED);
    }

    @Test
    void rejectsAdherenceForCancelledPlan() {
        rejectsAdherenceForTerminalPlan(CarePlanStatus.CANCELLED);
    }

    private void rejectsAdherenceForTerminalPlan(CarePlanStatus status) {
        CarePlan plan = CarePlan.builder().id("plan-1").patientId("patient-1").status(status).build();
        when(carePlanRepository.findById("plan-1")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service().markAdherence("plan-1", "int-1", AdherenceStatus.COMPLETED, null))
                .isInstanceOf(InvalidCarePlanStateException.class);
        verifyNoInteractions(adherenceRepository);
        verify(carePlanRepository, never()).save(any(CarePlan.class));
    }

    private CarePlanService service() {
        return new CarePlanService(carePlanRepository, adherenceRepository, outcomeRepository, riskRepository,
                alertRepository, vitalsRepository, auditService);
    }
}