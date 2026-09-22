package com.medisphere.careplan;

import com.medisphere.audit.AuditService;
import com.medisphere.exception.InvalidCarePlanStateException;
import com.medisphere.exception.ResourceNotFoundException;
import com.medisphere.monitoring.Alert;
import com.medisphere.monitoring.AlertStatus;
import com.medisphere.repository.AdherenceRecordRepository;
import com.medisphere.repository.AlertRepository;
import com.medisphere.repository.CarePlanRepository;
import com.medisphere.repository.OutcomeRepository;
import com.medisphere.repository.RiskPredictionRepository;
import com.medisphere.repository.VitalsRepository;
import com.medisphere.risk.RiskPrediction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarePlanService {
    private final CarePlanRepository carePlanRepository;
    private final AdherenceRecordRepository adherenceRepository;
    private final OutcomeRepository outcomeRepository;
    private final RiskPredictionRepository riskRepository;
    private final AlertRepository alertRepository;
    private final VitalsRepository vitalsRepository;
    private final AuditService auditService;

    public Page<CarePlan> list(String patientId, Pageable pageable) {
        return carePlanRepository.findByPatientIdOrderByUpdatedAtDesc(patientId, pageable);
    }

    public CarePlan active(String patientId) {
        return carePlanRepository.findFirstByPatientIdAndStatusOrderByUpdatedAtDesc(patientId, CarePlanStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active care plan", patientId));
    }

    public CarePlan get(String id) {
        CarePlan plan = carePlanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Care plan", id));
        auditService.record(AuditService.Actions.CARE_PLAN_VIEWED, "CarePlan", id, plan.getPatientId(), AuditService.Outcomes.SUCCESS, "Care plan viewed");
        return plan;
    }

    public CarePlan generate(String patientId) {
        return carePlanRepository.findFirstByPatientIdAndStatusOrderByUpdatedAtDesc(patientId, CarePlanStatus.ACTIVE)
                .orElseGet(() -> createPlan(patientId));
    }

    private CarePlan createPlan(String patientId) {
        List<RiskPrediction> risks = riskRepository.findByPatientIdOrderByGeneratedAtDesc(patientId, PageRequest.of(0, 20)).getContent();
        List<Alert> alerts = alertRepository.findByPatientIdAndStatusInOrderByCreatedAtDesc(patientId,
                List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED), PageRequest.of(0, 50)).getContent();
        List<String> reasons = new ArrayList<>();
        List<Goal> goals = new ArrayList<>();
        List<Intervention> interventions = new ArrayList<>();
        boolean cardiovascular = risks.stream().anyMatch(r -> "CARDIOVASCULAR".equalsIgnoreCase(r.getModelType()));
        boolean diabetes = risks.stream().anyMatch(r -> "DIABETES".equalsIgnoreCase(r.getModelType()));
        if (cardiovascular) {
            reasons.add("Generated from cardiovascular risk prediction");
            goals.add(goal("Maintain heart rate and blood pressure within configured monitoring ranges", "VITAL_MONITORING"));
            interventions.add(intervention(InterventionType.MONITORING, "Review cardiovascular measurements", "Review recorded cardiovascular measurements with the care team."));
        }
        if (diabetes) {
            reasons.add("Generated from diabetes risk prediction");
            goals.add(goal("Monitor glucose-related measurements", "LAB_MONITORING"));
            interventions.add(intervention(InterventionType.LAB_CHECK, "Review glucose-related measurements", "Review available glucose-related lab data with a clinician."));
        }
        if (!alerts.isEmpty()) {
            alerts.forEach(alert -> reasons.add("Generated from active " + alert.getAlertType() + " alert"));
            interventions.add(intervention(InterventionType.FOLLOW_UP, "Follow up on active monitoring alerts", "Review unresolved monitoring alerts with the care team."));
        }
        if (vitalsRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId).isPresent()) reasons.add("Generated from available vital measurements");
        if (reasons.isEmpty()) {
            reasons.add("Generated from baseline patient monitoring context");
            goals.add(goal("Complete scheduled health activities", "ACTIVITY_TRACKING"));
            interventions.add(intervention(InterventionType.MONITORING, "Continue routine monitoring", "Continue recording available health measurements."));
        }
        CarePlan plan = CarePlan.builder().id(UUID.randomUUID().toString()).patientId(patientId)
                .title("Personalized monitoring and follow-up plan")
                .description("Deterministic engineering/demo care-plan logic based on available patient context.")
                .status(CarePlanStatus.DRAFT).priority(alerts.stream().anyMatch(a -> a.getSeverity() != null && (a.getSeverity().name().equals("HIGH") || a.getSeverity().name().equals("CRITICAL"))) ? CarePlanPriority.HIGH : CarePlanPriority.MEDIUM)
                .createdAt(Instant.now()).updatedAt(Instant.now()).generatedAt(Instant.now()).source("DEMO_RULES")
                .generationReasons(reasons).basedOnRiskPredictionIds(risks.stream().map(RiskPrediction::getId).toList())
                .basedOnAlertIds(alerts.stream().map(Alert::getId).toList()).goals(goals).interventions(interventions).version(1).build();
        CarePlan saved = carePlanRepository.save(plan);
        auditService.record(AuditService.Actions.CARE_PLAN_CREATED, "CarePlan", saved.getId(), patientId, AuditService.Outcomes.SUCCESS, "Care plan generated from demonstration rules");
        return saved;
    }

    private Goal goal(String description, String type) {
        return Goal.builder().id(UUID.randomUUID().toString()).description(description).targetType(type).startDate(LocalDate.now()).status(GoalStatus.NOT_STARTED).progress(0.0).notes("Engineering/demo goal; not medical advice.").build();
    }

    private Intervention intervention(InterventionType type, String title, String description) {
        Instant now = Instant.now();
        return Intervention.builder().id(UUID.randomUUID().toString()).type(type).title(title).description(description).frequency("As configured by the care team").startDate(LocalDate.now()).status(InterventionStatus.PENDING).priority(CarePlanPriority.MEDIUM).instructions("Clinician review is required for real-world care decisions.").createdAt(now).updatedAt(now).build();
    }

    public CarePlan transition(String id, CarePlanStatus target) {
        CarePlan plan = get(id);
        if (!allowed(plan.getStatus(), target)) throw new InvalidCarePlanStateException("Cannot transition from " + plan.getStatus() + " to " + target);
        CarePlanStatus previous = plan.getStatus();
        plan.setStatus(target); plan.setUpdatedAt(Instant.now());
        CarePlan saved = carePlanRepository.save(plan);
        String action = switch (target) { case ACTIVE -> AuditService.Actions.CARE_PLAN_ACTIVATED; case PAUSED -> AuditService.Actions.CARE_PLAN_PAUSED; case COMPLETED -> AuditService.Actions.CARE_PLAN_COMPLETED; case CANCELLED -> AuditService.Actions.CARE_PLAN_CANCELLED; default -> AuditService.Actions.CARE_PLAN_VIEWED; };
        auditService.record(action, "CarePlan", id, plan.getPatientId(), AuditService.Outcomes.SUCCESS, "Status " + previous + " -> " + target);
        return saved;
    }

    private boolean allowed(CarePlanStatus from, CarePlanStatus to) {
        return (from == CarePlanStatus.DRAFT && to == CarePlanStatus.ACTIVE)
                || (from == CarePlanStatus.ACTIVE && (to == CarePlanStatus.PAUSED || to == CarePlanStatus.COMPLETED || to == CarePlanStatus.CANCELLED))
                || (from == CarePlanStatus.PAUSED && to == CarePlanStatus.ACTIVE);
    }

    public CarePlan markAdherence(String id, String interventionId, AdherenceStatus status, String notes) {
        CarePlan plan = get(id);
        if (plan.getStatus() == CarePlanStatus.COMPLETED || plan.getStatus() == CarePlanStatus.CANCELLED) {
            throw new InvalidCarePlanStateException("Cannot update adherence for a " + plan.getStatus() + " care plan");
        }
        Intervention intervention = plan.getInterventions().stream().filter(item -> interventionId.equals(item.getId())).findFirst().orElseThrow(() -> new ResourceNotFoundException("Intervention", interventionId));
        LocalDate date = LocalDate.now();
        AdherenceRecord record = adherenceRepository.findByInterventionIdAndDate(interventionId, date).orElseGet(() -> AdherenceRecord.builder().id(UUID.randomUUID().toString()).interventionId(interventionId).carePlanId(id).patientId(plan.getPatientId()).date(date).build());
        record.setStatus(status); record.setNotes(notes); record.setActor(actor()); record.setCompletedAt(status == AdherenceStatus.COMPLETED ? Instant.now() : null);
        adherenceRepository.save(record);
        intervention.setStatus(status == AdherenceStatus.COMPLETED ? InterventionStatus.COMPLETED : status == AdherenceStatus.MISSED ? InterventionStatus.MISSED : InterventionStatus.PENDING);
        refreshSummary(plan); carePlanRepository.save(plan);
        auditService.record(status == AdherenceStatus.COMPLETED ? AuditService.Actions.INTERVENTION_COMPLETED : AuditService.Actions.INTERVENTION_MISSED, "Intervention", interventionId, plan.getPatientId(), AuditService.Outcomes.SUCCESS, "Intervention adherence updated");
        return plan;
    }

    public CarePlan.AdherenceSummary adherence(String id) { CarePlan plan = get(id); refreshSummary(plan); return plan.getAdherenceSummary(); }

    public Outcome addOutcome(String id, String goalId, Double value, String unit, String notes, String source, OutcomeStatus status) {
        CarePlan plan = get(id);
        if (plan.getGoals().stream().noneMatch(goal -> goalId.equals(goal.getId()))) throw new ResourceNotFoundException("Goal", goalId);
        Outcome outcome = Outcome.builder().id(UUID.randomUUID().toString()).patientId(plan.getPatientId()).carePlanId(id).goalId(goalId).measuredValue(value).unit(unit).recordedAt(Instant.now()).notes(notes).source(source == null ? "DEMO_MEASUREMENT" : source).status(status).build();
        Outcome saved = outcomeRepository.save(outcome);
        List<Outcome> outcomes = outcomeRepository.findByCarePlanIdOrderByRecordedAtDesc(id);
        plan.setOutcomeSummary(new CarePlan.OutcomeSummary(outcomes.size(), outcomes.stream().filter(item -> item.getStatus() == OutcomeStatus.ACHIEVED).count()));
        carePlanRepository.save(plan);
        auditService.record(AuditService.Actions.OUTCOME_RECORDED, "Outcome", saved.getId(), plan.getPatientId(), AuditService.Outcomes.SUCCESS, "Outcome recorded");
        return saved;
    }

    public List<Outcome> outcomes(String id) { get(id); return outcomeRepository.findByCarePlanIdOrderByRecordedAtDesc(id); }

    private void refreshSummary(CarePlan plan) {
        List<AdherenceRecord> records = adherenceRepository.findByCarePlanId(plan.getId());
        long completed = records.stream().filter(r -> r.getStatus() == AdherenceStatus.COMPLETED).count();
        long missed = records.stream().filter(r -> r.getStatus() == AdherenceStatus.MISSED).count();
        long pending = records.stream().filter(r -> r.getStatus() == AdherenceStatus.PENDING).count();
        long denominator = completed + missed;
        plan.setAdherenceSummary(new CarePlan.AdherenceSummary(completed, missed, pending, denominator == 0 ? 0 : completed * 100.0 / denominator));
    }

    private String actor() { var auth = SecurityContextHolder.getContext().getAuthentication(); return auth == null ? "SYSTEM" : auth.getName(); }
}