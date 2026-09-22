package com.medisphere.repository;

import com.medisphere.careplan.CarePlan;
import com.medisphere.careplan.CarePlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CarePlanRepository extends MongoRepository<CarePlan, String> {
    Page<CarePlan> findByPatientIdOrderByUpdatedAtDesc(String patientId, Pageable pageable);
    Optional<CarePlan> findFirstByPatientIdAndStatusOrderByUpdatedAtDesc(String patientId, CarePlanStatus status);
}