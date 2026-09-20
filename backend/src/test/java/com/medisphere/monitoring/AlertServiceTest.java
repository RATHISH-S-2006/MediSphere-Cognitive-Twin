package com.medisphere.monitoring;

import com.medisphere.audit.AuditService;
import com.medisphere.kafka.event.VitalEvent;
import com.medisphere.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock private AlertRepository repository;
    @Mock private AuditService auditService;

    @Test
    void suppressesRepeatedViolationForSameActiveRule() {
        MonitoringRuleProperties properties = properties();
        AlertService service = new AlertService(repository, new MonitoringService(properties), auditService);
        when(repository.findFirstByPatientIdAndAlertTypeAndVitalTypeAndStatus(
                "patient-1", "SPO2_LOW", VitalType.SPO2, AlertStatus.ACTIVE)).thenReturn(Optional.of(activeAlert()));

        service.process(VitalEvent.builder().patientId("patient-1").source("TEST").spo2(88.0).build());

        verify(repository, never()).save(any(Alert.class));
    }

    @Test
    void assignsServerCreationTimestampToNewAlert() {
        MonitoringRuleProperties properties = properties();
        AlertService service = new AlertService(repository, new MonitoringService(properties), auditService);
        when(repository.findFirstByPatientIdAndAlertTypeAndVitalTypeAndStatus(
                "patient-1", "SPO2_LOW", VitalType.SPO2, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
        when(repository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        Alert created = service.process(VitalEvent.builder().patientId("patient-1").source("TEST").spo2(88.0).build())
                .getFirst();

        assertThat(created.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void enforcesActiveToAcknowledgedToResolvedLifecycle() {
        Alert active = activeAlert();
        when(repository.findById("alert-1")).thenReturn(Optional.of(active));
        when(repository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AlertService service = new AlertService(repository, new MonitoringService(properties()), auditService);

        assertThat(service.acknowledge("alert-1").getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(service.resolve("alert-1").getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThatThrownBy(() -> service.acknowledge("alert-1"))
                .isInstanceOf(InvalidAlertStateException.class);
    }

    private MonitoringRuleProperties properties() {
        MonitoringRuleProperties properties = new MonitoringRuleProperties();
        MonitoringRuleProperties.RuleConfig rule = new MonitoringRuleProperties.RuleConfig();
        rule.setAlertType("SPO2_LOW");
        rule.setVitalType(VitalType.SPO2);
        rule.setSeverity(AlertSeverity.CRITICAL);
        rule.setOperator(MonitoringOperator.LESS_THAN);
        rule.setThreshold(92);
        properties.setRules(List.of(rule));
        return properties;
    }

    private Alert activeAlert() {
        return Alert.builder().id("alert-1").patientId("patient-1").alertType("SPO2_LOW")
                .vitalType(VitalType.SPO2).status(AlertStatus.ACTIVE).build();
    }
}
