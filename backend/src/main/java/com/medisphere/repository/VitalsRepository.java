package com.medisphere.repository;

import com.medisphere.domain.Vitals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VitalsRepository extends MongoRepository<Vitals, String> {
    Optional<Vitals> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
    Page<Vitals> findByPatientIdOrderByRecordedAtDesc(String patientId, Pageable pageable);
    Optional<Vitals> findTopByPatientIdOrderByRecordedAtDesc(String patientId);
}
