package com.medisphere.repository;

import com.medisphere.risk.RiskPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskPredictionRepository extends MongoRepository<RiskPrediction, String> {
    Page<RiskPrediction> findByPatientIdOrderByGeneratedAtDesc(String patientId, Pageable pageable);
    Page<RiskPrediction> findByPatientIdAndModelTypeOrderByGeneratedAtDesc(String patientId, String modelType, Pageable pageable);
}
