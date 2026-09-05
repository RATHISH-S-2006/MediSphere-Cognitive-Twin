package com.medisphere.security;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SMART on FHIR foundation security configuration.
 *
 * Security flow enforced here:
 * Authentication → RBAC (via @PreAuthorize) → Business operation → Audit event
 *
 * For development: a permissive JWT configuration is used.
 * For production: configure spring.security.oauth2.resourceserver.jwt.issuer-uri
 *   to point to a real SMART-compatible identity provider.
 *
 * IMPORTANT: Method-level security (@PreAuthorize) is the primary RBAC mechanism.
 * Do NOT rely only on Angular route guards for authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @Profile("dev")
    public JwtDecoder devJwtDecoder() {
        return token -> {
            String[] parts = token.split("-", 2);
            String role = parts.length == 2 ? parts[0].toUpperCase() : "ADMIN";
            String subject = parts.length == 2 ? parts[1] : "dev-user";
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("roles", List.of(role))
                .claim("scope", "openid fhirUser patient/*.read user/*.read")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Stateless JWT - no CSRF needed
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public health/readiness endpoints
                .requestMatchers("/api/health/**", "/actuator/health/**").permitAll()
                // Swagger/OpenAPI (dev convenience)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new MediSphereJwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
