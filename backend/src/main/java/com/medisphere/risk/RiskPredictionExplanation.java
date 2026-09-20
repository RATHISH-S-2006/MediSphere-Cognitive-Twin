package com.medisphere.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskPredictionExplanation {
    private String feature;
    private String label;
    private double value;
    private double shapValue;
    private String impact;
}
