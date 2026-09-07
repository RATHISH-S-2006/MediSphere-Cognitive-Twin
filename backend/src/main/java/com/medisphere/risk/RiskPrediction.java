package com.medisphere.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "risk_predictions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskPrediction {

    @Id
    private String id;

    @Indexed
    private String patientId;

    @Indexed
    private String modelType;

    private double riskScore;
    private String riskCategory;
    private String modelVersion;

    @Builder.Default
    private List<RiskPredictionExplanation> explanations = new ArrayList<>();

    private Instant generatedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private String source;
}
