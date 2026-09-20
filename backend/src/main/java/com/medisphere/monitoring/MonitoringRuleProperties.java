package com.medisphere.monitoring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "medisphere.monitoring")
public class MonitoringRuleProperties {

    private List<RuleConfig> rules = new ArrayList<>();

    @Getter
    @Setter
    public static class RuleConfig {
        private String alertType;
        private VitalType vitalType;
        private AlertSeverity severity;
        private MonitoringOperator operator;
        private double threshold;
        private boolean enabled = true;

        public MonitoringRule toRule() {
            return new MonitoringRule(alertType, vitalType, severity, operator, threshold, enabled);
        }
    }
}
