package com.medisphere.repository;

import com.medisphere.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByFhirPatientId(String fhirPatientId);
    Optional<Patient> findByMrn(String mrn);
    Page<Patient> findByActiveTrue(Pageable pageable);
    Page<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);
    boolean existsByFhirPatientId(String fhirPatientId);
}
