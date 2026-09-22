package com.medisphere.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Intervention {
    private String id;
    private InterventionType type;
    private String title;
    private String description;
    private String frequency;
    private LocalTime scheduledTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private InterventionStatus status;
    private CarePlanPriority priority;
    private String instructions;
    private int completionCount;
    private int missedCount;
    private Instant createdAt;
    private Instant updatedAt;
}