package com.medisphere.monitoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "patient_status_created_idx", def = "{'patientId': 1, 'status': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "patient_rule_status_idx", def = "{'patientId': 1, 'alertType': 1, 'vitalType': 1, 'status': 1}")
})
public class Alert {
    @Id
    private String id;
    @Indexed
    private String patientId;
    private String alertType;
    private VitalType vitalType;
    private double observedValue;
    private double threshold;
    private MonitoringOperator operator;
    private AlertSeverity severity;
    private String message;
    private String source;
    @Indexed
    private AlertStatus status;
    private String acknowledgedBy;
    private Instant acknowledgedAt;
    private String resolvedBy;
    private Instant resolvedAt;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
