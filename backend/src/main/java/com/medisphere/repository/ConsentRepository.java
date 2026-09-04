package com.medisphere.repository;

import com.medisphere.domain.Consent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentRepository extends MongoRepository<Consent, String> {
    List<Consent> findByPatientIdOrderByCreatedAtDesc(String patientId);
    Optional<Consent> findTopByPatientIdAndStatusOrderByGrantedAtDesc(String patientId, Consent.ConsentStatus status);
}
