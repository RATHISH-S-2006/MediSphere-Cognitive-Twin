package com.medisphere.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Custom JWT authentication converter that extracts MediSphere roles
 * from the JWT token issued by a SMART on FHIR compatible identity provider.
 *
 * Roles are extracted from the "roles" claim in the JWT.
 * SMART scopes (patient/*.read, user/*.read, fhirUser) are extracted from "scope" claim.
 * All roles are prefixed with "ROLE_" to work with Spring Security's @PreAuthorize.
 *
 * Roles supported: PATIENT, PROVIDER, ADMIN, CLINICIAN
 */
public class MediSphereJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Extract custom roles from "roles" claim
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .forEach(authorities::add);
        }

        // Extract SMART on FHIR scopes from "scope" claim
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            for (String s : scope.split(" ")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
            }
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
