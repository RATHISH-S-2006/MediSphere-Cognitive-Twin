package com.medisphere.repository;

import com.medisphere.domain.FHIRResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FHIRResourceRepository extends MongoRepository<FHIRResource, String> {
    Optional<FHIRResource> findByFhirResourceIdAndResourceType(String fhirResourceId, String resourceType);
    boolean existsByFhirResourceIdAndResourceType(String fhirResourceId, String resourceType);
    Page<FHIRResource> findByPatientId(String patientId, Pageable pageable);
    Page<FHIRResource> findByPatientIdAndResourceType(String patientId, String resourceType, Pageable pageable);
}
