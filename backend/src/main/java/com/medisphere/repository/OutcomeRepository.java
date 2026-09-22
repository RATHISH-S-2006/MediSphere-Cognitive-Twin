package com.medisphere.repository;

import com.medisphere.careplan.Outcome;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutcomeRepository extends MongoRepository<Outcome, String> {
    List<Outcome> findByCarePlanIdOrderByRecordedAtDesc(String carePlanId);
}