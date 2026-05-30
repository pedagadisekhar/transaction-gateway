package com.swiftpay.transaction_gateway.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.transaction_gateway.entity.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String PAYMENT_INITIATED_TOPIC = "payment-initiated";
    private static final String PAYMENT_STATUS_TOPIC = "payment-status";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishPaymentInitiated(Transaction transaction) {
        sendEvent(PAYMENT_INITIATED_TOPIC, transaction.getTransactionId(), transaction);
    }

    public void publishPaymentStatus(String transactionId, Object statusPayload) {
        sendEvent(PAYMENT_STATUS_TOPIC, transactionId, statusPayload);
    }

    private void sendEvent(String topic, String key, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message);
            logger.info("Published Kafka event topic={} key={}", topic, key);
        } catch (JsonProcessingException ex) {
            logger.error("Unable to serialize Kafka event for topic={}", topic, ex);
            throw new IllegalStateException("Failed to publish Kafka event", ex);
        }
    }
}
