package com.medisphere.repository;

import com.medisphere.careplan.AdherenceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdherenceRecordRepository extends MongoRepository<AdherenceRecord, String> {
    Optional<AdherenceRecord> findByInterventionIdAndDate(String interventionId, LocalDate date);
    List<AdherenceRecord> findByCarePlanId(String carePlanId);
}