package com.medisphere.monitoring;

import com.medisphere.kafka.event.VitalEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringService {
    private final MonitoringRuleProperties properties;

    public List<MonitoringEvaluation> evaluate(VitalEvent event) {
        List<MonitoringEvaluation> evaluations = new ArrayList<>();
        for (MonitoringRuleProperties.RuleConfig config : properties.getRules()) {
            MonitoringRule rule = config.toRule();
            Double observed = observedValue(event, rule.vitalType());
            if (rule.enabled() && observed != null && violates(observed, rule)) {
                evaluations.add(new MonitoringEvaluation(rule, observed));
            }
        }
        return evaluations;
    }

    private Double observedValue(VitalEvent event, VitalType type) {
        return switch (type) {
            case HEART_RATE -> number(event.getHeartRate());
            case SPO2 -> event.getSpo2();
            case TEMPERATURE -> event.getTemperature();
            case SYSTOLIC_BLOOD_PRESSURE -> number(event.getSystolicBp());
            case DIASTOLIC_BLOOD_PRESSURE -> number(event.getDiastolicBp());
        };
    }

    private boolean violates(double observed, MonitoringRule rule) {
        return switch (rule.operator()) {
            case GREATER_THAN -> observed > rule.threshold();
            case LESS_THAN -> observed < rule.threshold();
            case GREATER_THAN_OR_EQUAL -> observed >= rule.threshold();
            case LESS_THAN_OR_EQUAL -> observed <= rule.threshold();
        };
    }

    private Double number(Number value) {
        return value == null ? null : value.doubleValue();
    }
}
