package com.medisphere.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "care_plan_adherence")
@CompoundIndex(name = "intervention_date_idx", def = "{'interventionId': 1, 'date': 1}", unique = true)
public class AdherenceRecord {
    @Id
    private String id;
    private String interventionId;
    private String carePlanId;
    private String patientId;
    private LocalDate date;
    private AdherenceStatus status;
    private Instant completedAt;
    private String notes;
    private String actor;
}