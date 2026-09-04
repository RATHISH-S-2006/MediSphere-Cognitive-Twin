package com.medisphere.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Healthcare Provider entity.
 * M3 will use providers for alert routing.
 * M4 will use providers for careplan collaboration.
 */
@Document(collection = "providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provider {

    @Id
    private String id; // UUID

    @Indexed(unique = true)
    private String npi; // National Provider Identifier if available

    private String firstName;
    private String lastName;
    private String specialty;
    private String email;
    private String phone;
    private String organization;
    private String department;

    private boolean active;

    // Patient IDs this provider is authorized to access
    @Builder.Default
    private List<String> authorizedPatientIds = new ArrayList<>();

    @Builder.Default
    private String schemaVersion = "1.0";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
