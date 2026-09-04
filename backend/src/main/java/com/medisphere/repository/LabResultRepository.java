package com.medisphere.repository;

import com.medisphere.domain.LabResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabResultRepository extends MongoRepository<LabResult, String> {
    boolean existsByFhirObservationId(String fhirObservationId);
    Optional<LabResult> findByFhirObservationId(String fhirObservationId);
    Page<LabResult> findByPatientIdOrderByCollectedAtDesc(String patientId, Pageable pageable);
}
