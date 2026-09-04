package com.medisphere.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import static com.medisphere.kafka.event.KafkaTopics.*;

/**
 * Kafka infrastructure configuration.
 * Topics are created automatically if they don't exist.
 * ObjectMapper with JavaTimeModule is provided for JSON serialization of Instant fields.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic fhirResourcesTopic() {
        return TopicBuilder.name(FHIR_RESOURCES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic vitalsTopic() {
        return TopicBuilder.name(VITALS)
                .partitions(6) // Higher partition count for M3 real-time parallelism
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic twinUpdatesTopic() {
        return TopicBuilder.name(TWIN_UPDATES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
