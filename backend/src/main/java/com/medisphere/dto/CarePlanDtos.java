package com.medisphere.dto;

import com.medisphere.careplan.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class CarePlanDtos {
    private CarePlanDtos() {}

    public record AdherenceRequest(@NotNull AdherenceStatus status, String notes) {}
    public record OutcomeRequest(@NotNull String goalId, @NotNull Double measuredValue, String unit,
                                 String notes, String source, @NotNull OutcomeStatus status) {}
    public record CarePlanResponse(String id, String patientId, String title, String description,
                                   CarePlanStatus status, CarePlanPriority priority, Instant createdAt,
                                   Instant updatedAt, Instant generatedAt, String source,
                                   List<String> generationReasons, List<String> basedOnRiskPredictionIds,
                                   List<String> basedOnAlertIds, List<Goal> goals, List<Intervention> interventions,
                                   CarePlan.AdherenceSummary adherenceSummary, CarePlan.OutcomeSummary outcomeSummary,
                                   int version) {
        public static CarePlanResponse from(CarePlan plan) {
            return new CarePlanResponse(plan.getId(), plan.getPatientId(), plan.getTitle(), plan.getDescription(),
                    plan.getStatus(), plan.getPriority(), plan.getCreatedAt(), plan.getUpdatedAt(), plan.getGeneratedAt(),
                    plan.getSource(), plan.getGenerationReasons(), plan.getBasedOnRiskPredictionIds(),
                    plan.getBasedOnAlertIds(), plan.getGoals(), plan.getInterventions(), plan.getAdherenceSummary(),
                    plan.getOutcomeSummary(), plan.getVersion());
        }
    }

    public record OutcomeResponse(String id, String patientId, String carePlanId, String goalId,
                                  Double measuredValue, String unit, Instant recordedAt, String notes,
                                  String source, OutcomeStatus status) {
        public static OutcomeResponse from(Outcome outcome) {
            return new OutcomeResponse(outcome.getId(), outcome.getPatientId(), outcome.getCarePlanId(), outcome.getGoalId(),
                    outcome.getMeasuredValue(), outcome.getUnit(), outcome.getRecordedAt(), outcome.getNotes(),
                    outcome.getSource(), outcome.getStatus());
        }
    }
}