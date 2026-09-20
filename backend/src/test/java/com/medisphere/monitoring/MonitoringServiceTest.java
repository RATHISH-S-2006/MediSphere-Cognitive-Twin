package com.medisphere.monitoring;

import com.medisphere.kafka.event.VitalEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringServiceTest {
    @Test
    void evaluatesConfiguredAbnormalVitalsWithObservedValueAndThreshold() {
        MonitoringRuleProperties properties = new MonitoringRuleProperties();
        MonitoringRuleProperties.RuleConfig rule = new MonitoringRuleProperties.RuleConfig();
        rule.setAlertType("SPO2_LOW");
        rule.setVitalType(VitalType.SPO2);
        rule.setSeverity(AlertSeverity.CRITICAL);
        rule.setOperator(MonitoringOperator.LESS_THAN);
        rule.setThreshold(92);
        properties.setRules(List.of(rule));

        List<MonitoringEvaluation> evaluations = new MonitoringService(properties).evaluate(VitalEvent.builder()
                .patientId("patient-1").timestamp(Instant.now()).spo2(89.0).build());

        assertThat(evaluations).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.observedValue()).isEqualTo(89.0);
            assertThat(evaluation.rule().threshold()).isEqualTo(92.0);
            assertThat(evaluation.rule().severity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(evaluation.message()).contains("observed 89", "threshold 92");
        });
    }

    @Test
    void doesNotEvaluateNormalOrMissingVitals() {
        MonitoringRuleProperties properties = new MonitoringRuleProperties();
        MonitoringRuleProperties.RuleConfig rule = new MonitoringRuleProperties.RuleConfig();
        rule.setAlertType("HEART_RATE_HIGH");
        rule.setVitalType(VitalType.HEART_RATE);
        rule.setSeverity(AlertSeverity.HIGH);
        rule.setOperator(MonitoringOperator.GREATER_THAN);
        rule.setThreshold(120);
        properties.setRules(List.of(rule));

        MonitoringService service = new MonitoringService(properties);
        assertThat(service.evaluate(VitalEvent.builder().heartRate(80).build())).isEmpty();
        assertThat(service.evaluate(VitalEvent.builder().build())).isEmpty();
    }
}
