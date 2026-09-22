package com.medisphere.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "care_plan_outcomes")
public class Outcome {
    @Id
    private String id;
    private String patientId;
    private String carePlanId;
    private String goalId;
    private Double measuredValue;
    private String unit;
    private Instant recordedAt;
    private String notes;
    private String source;
    private OutcomeStatus status;
}