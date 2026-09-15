package com.medisphere.risk;

import com.medisphere.consent.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskPredictionController {

    private final RiskPredictionService riskPredictionService;
    private final ConsentService consentService;

    @PostMapping("/{patientId}/cardiovascular/predict")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ResponseEntity<RiskPredictionResponse> predictCardiovascular(@PathVariable String patientId,
                                                                      @Valid @RequestBody RiskPredictionRequest request) {
        consentService.verifyConsent(patientId);
        return ResponseEntity.ok(riskPredictionService.buildAndPersistPrediction(patientId, request));
    }

    @PostMapping("/{patientId}/diabetes/predict")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ResponseEntity<RiskPredictionResponse> predictDiabetes(@PathVariable String patientId,
                                                                @Valid @RequestBody RiskPredictionRequest request) {
        consentService.verifyConsent(patientId);
        return ResponseEntity.ok(riskPredictionService.buildAndPersistPrediction(patientId, request));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ResponseEntity<Page<RiskPrediction>> getHistory(@PathVariable String patientId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        consentService.verifyConsent(patientId);
        return ResponseEntity.ok(riskPredictionService.listHistory(patientId, PageRequest.of(page, size)));
    }

    @GetMapping("/{patientId}/latest")
    @PreAuthorize("hasAnyRole('PATIENT', 'PROVIDER', 'CLINICIAN', 'ADMIN') && @patientAccessChecker.canAccess(#patientId)")
    public ResponseEntity<RiskPrediction> getLatest(@PathVariable String patientId,
                                                     @RequestParam(defaultValue = "CARDIOVASCULAR") String modelType) {
        consentService.verifyConsent(patientId);
        return ResponseEntity.ok(riskPredictionService.getLatest(patientId, modelType));
    }
}
