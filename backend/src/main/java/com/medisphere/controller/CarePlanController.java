package com.medisphere.controller;

import com.medisphere.careplan.*;
import com.medisphere.consent.ConsentService;
import com.medisphere.dto.CarePlanDtos;
import com.medisphere.security.PatientAccessChecker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/care-plans")
@RequiredArgsConstructor
public class CarePlanController {
    private final CarePlanService service;
    private final PatientAccessChecker accessChecker;
    private final ConsentService consentService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public Page<CarePlanDtos.CarePlanResponse> list(@PathVariable String patientId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(patientId, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))).map(CarePlanDtos.CarePlanResponse::from);
    }

    @GetMapping("/{patientId}/active")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public CarePlanDtos.CarePlanResponse active(@PathVariable String patientId) { return CarePlanDtos.CarePlanResponse.from(service.active(patientId)); }

    @PostMapping("/{patientId}/generate")
    @PreAuthorize("hasAnyRole('PROVIDER','CLINICIAN','ADMIN') && @patientAccessChecker.canAccess(#patientId) && @consentService.hasActiveConsent(#patientId)")
    public CarePlanDtos.CarePlanResponse generate(@PathVariable String patientId) { return CarePlanDtos.CarePlanResponse.from(service.generate(patientId)); }

    @GetMapping("/by-id/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN')")
    public CarePlanDtos.CarePlanResponse get(@PathVariable String id) { return CarePlanDtos.CarePlanResponse.from(authorize(service.get(id))); }

    @PostMapping("/by-id/{id}/{action}")
    @PreAuthorize("hasAnyRole('PROVIDER','CLINICIAN','ADMIN')")
    public CarePlanDtos.CarePlanResponse transition(@PathVariable String id, @PathVariable String action) {
        CarePlan plan = authorize(service.get(id));
        CarePlanStatus status = switch (action.toLowerCase()) { case "activate" -> CarePlanStatus.ACTIVE; case "pause" -> CarePlanStatus.PAUSED; case "complete" -> CarePlanStatus.COMPLETED; case "cancel" -> CarePlanStatus.CANCELLED; default -> throw new IllegalArgumentException("Unknown care-plan action"); };
        return CarePlanDtos.CarePlanResponse.from(service.transition(plan.getId(), status));
    }

    @PostMapping("/by-id/{id}/interventions/{interventionId}/{action}")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN')")
    public CarePlanDtos.CarePlanResponse adherence(@PathVariable String id, @PathVariable String interventionId, @PathVariable String action, @RequestBody(required = false) CarePlanDtos.AdherenceRequest request) {
        CarePlan plan = authorize(service.get(id));
        AdherenceStatus status = switch (action.toLowerCase()) {
            case "complete" -> AdherenceStatus.COMPLETED;
            case "miss" -> AdherenceStatus.MISSED;
            default -> throw new IllegalArgumentException("Unknown adherence action");
        };
        return CarePlanDtos.CarePlanResponse.from(service.markAdherence(plan.getId(), interventionId, status, request == null ? null : request.notes()));
    }

    @GetMapping("/by-id/{id}/adherence")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN')")
    public CarePlan.AdherenceSummary adherence(@PathVariable String id) { return service.adherence(authorize(service.get(id)).getId()); }

    @PostMapping("/by-id/{id}/outcomes")
    @PreAuthorize("hasAnyRole('PROVIDER','CLINICIAN','ADMIN')")
    public CarePlanDtos.OutcomeResponse outcome(@PathVariable String id, @Valid @RequestBody CarePlanDtos.OutcomeRequest request) {
        CarePlan plan = authorize(service.get(id));
        return CarePlanDtos.OutcomeResponse.from(service.addOutcome(plan.getId(), request.goalId(), request.measuredValue(), request.unit(), request.notes(), request.source(), request.status()));
    }

    @GetMapping("/by-id/{id}/outcomes")
    @PreAuthorize("hasAnyRole('PATIENT','PROVIDER','CLINICIAN','ADMIN')")
    public List<CarePlanDtos.OutcomeResponse> outcomes(@PathVariable String id) { return service.outcomes(authorize(service.get(id)).getId()).stream().map(CarePlanDtos.OutcomeResponse::from).toList(); }

    private CarePlan authorize(CarePlan plan) { if (!accessChecker.canAccess(plan.getPatientId())) throw new AccessDeniedException("Care-plan access denied"); consentService.verifyConsent(plan.getPatientId()); return plan; }
}