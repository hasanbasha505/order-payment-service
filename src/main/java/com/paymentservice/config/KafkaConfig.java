package com.paymentservice.config;

import com.paymentservice.events.PaymentEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for payment event publishing.
 */
@Configuration
@EnableScheduling
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    /**
     * Create the payment-events topic.
     */
    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
                .partitions(6)
                .replicas(1)  // Use 3 in production
                .config("retention.ms", "604800000")  // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Create the payment-dlq topic for failed events.
     */
    @Bean
    public NewTopic paymentDlqTopic() {
        return TopicBuilder.name("payment-events-dlq")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000")  // 30 days
                .build();
    }

    /**
     * Producer factory with idempotent producer configuration.
     */
    @Bean
    public ProducerFactory<String, PaymentEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Batching for better throughput
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending payment events.
     */
    @Bean
    public KafkaTemplate<String, PaymentEvent> kafkaTemplate() {
        KafkaTemplate<String, PaymentEvent> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic("payment-events");
        return template;
    }
}
