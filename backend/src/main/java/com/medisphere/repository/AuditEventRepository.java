package com.medisphere.repository;

import com.medisphere.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    Page<AuditEvent> findByPatientIdOrderByTimestampDesc(String patientId, Pageable pageable);
    Page<AuditEvent> findByActorIdOrderByTimestampDesc(String actorId, Pageable pageable);
    Page<AuditEvent> findByActionOrderByTimestampDesc(String action, Pageable pageable);
    Page<AuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);
}
