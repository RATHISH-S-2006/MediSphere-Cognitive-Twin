package com.medisphere.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "care_plans")
@CompoundIndex(name = "patient_status_idx", def = "{'patientId': 1, 'status': 1, 'updatedAt': -1}")
public class CarePlan {
    @Id
    private String id;
    @Indexed
    private String patientId;
    private String title;
    private String description;
    private CarePlanStatus status;
    private CarePlanPriority priority;
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    private Instant generatedAt;
    private String source;
    @Builder.Default
    private List<String> generationReasons = new ArrayList<>();
    @Builder.Default
    private List<String> basedOnRiskPredictionIds = new ArrayList<>();
    @Builder.Default
    private List<String> basedOnAlertIds = new ArrayList<>();
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();
    @Builder.Default
    private List<Intervention> interventions = new ArrayList<>();
    @Builder.Default
    private AdherenceSummary adherenceSummary = new AdherenceSummary();
    @Builder.Default
    private OutcomeSummary outcomeSummary = new OutcomeSummary();
    private int version;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdherenceSummary {
        private long completed;
        private long missed;
        private long pending;
        private double adherencePercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeSummary {
        private long total;
        private long achieved;
    }
}