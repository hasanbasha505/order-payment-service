package com.paymentservice.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentservice.enums.OutboxStatus;
import com.paymentservice.models.OutboxEvent;
import com.paymentservice.models.Payment;
import com.paymentservice.models.PaymentTransaction;
import com.paymentservice.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes payment events to Kafka using the transactional outbox pattern.
 * Events are first written to the outbox table within the same transaction,
 * then published to Kafka by a background poller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Queue a payment event for publishing via the outbox.
     * This should be called within the same transaction as the payment update.
     */
    @Transactional
    public void queueEvent(PaymentEventType eventType, Payment payment, PaymentTransaction transaction,
                           String idempotencyKey) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(getEventAmount(eventType, payment, transaction))
                    .currencyCode(payment.getCurrencyCode())
                    .paymentStatus(payment.getStatus().name())
                    .transactionId(transaction != null ? transaction.getId() : null)
                    .providerTransactionId(transaction != null ? transaction.getProviderTxnId() : null)
                    .occurredAt(Instant.now())
                    .idempotencyKey(idempotencyKey)
                    .build();

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("PAYMENT")
                    .aggregateId(payment.getId())
                    .eventType(eventType.name())
                    .payload(Map.of("event", payload))
                    .status(OutboxStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Queued outbox event: {} for payment: {}", eventType, payment.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event", e);
            throw new RuntimeException("Failed to queue payment event", e);
        }
    }

    /**
     * Background job to publish pending outbox events to Kafka.
     * Runs every 100ms as configured.
     */
    @Scheduled(fixedDelayString = "${payment.outbox.polling-interval-ms:100}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEventsForPublishing(100);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                String eventPayload = (String) outboxEvent.getPayload().get("event");
                PaymentEvent event = objectMapper.readValue(eventPayload, PaymentEvent.class);

                kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, event.getPaymentId().toString(), event)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event to Kafka: {}", outboxEvent.getId(), ex);
                            } else {
                                log.debug("Published event to Kafka: {}", outboxEvent.getId());
                            }
                        });

                outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                outboxEvent.setPublishedAt(Instant.now());
                outboxEventRepository.save(outboxEvent);

            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", outboxEvent.getId(), e);
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);

                if (outboxEvent.getRetryCount() >= 5) {
                    outboxEvent.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event exceeded max retries, marking as failed: {}", outboxEvent.getId());
                }

                outboxEventRepository.save(outboxEvent);
            }
        }
    }

    private Long getEventAmount(PaymentEventType eventType, Payment payment, PaymentTransaction transaction) {
        if (transaction != null && transaction.getAmount() != null) {
            return transaction.getAmount();
        }

        return switch (eventType) {
            case PAYMENT_AUTHORIZED -> payment.getAuthorizedAmount();
            case PAYMENT_CAPTURED -> payment.getCapturedAmount();
            case PAYMENT_REFUNDED -> payment.getRefundedAmount();
            default -> null;
        };
    }
}
