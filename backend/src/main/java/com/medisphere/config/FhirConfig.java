package com.medisphere.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

/**
 * FHIR R4 client configuration.
 * The FHIR server URL is fully configurable via FHIR_BASE_URL environment variable.
 * Business logic must NOT be coupled to HAPI-specific implementation details.
 * Switching from HAPI FHIR to a real EHR endpoint requires only configuration change.
 */
@Configuration
public class FhirConfig {

    @Value("${medisphere.fhir.base-url}")
    private String fhirBaseUrl;

    @Bean
    public FhirContext fhirContext() {
        FhirContext ctx = FhirContext.forR4();
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ctx.getRestfulClientFactory().setConnectTimeout(30_000);
        ctx.getRestfulClientFactory().setSocketTimeout(60_000);
        return ctx;
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        return fhirContext.newRestfulGenericClient(fhirBaseUrl);
    }
}
