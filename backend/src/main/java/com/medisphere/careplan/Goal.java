package com.medisphere.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {
    private String id;
    private String description;
    private String targetType;
    private Double targetValue;
    private String unit;
    private LocalDate startDate;
    private LocalDate targetDate;
    private GoalStatus status;
    private Double progress;
    private String notes;
}