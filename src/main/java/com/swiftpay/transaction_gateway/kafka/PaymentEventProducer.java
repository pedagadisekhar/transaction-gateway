package com.swiftpay.transaction_gateway.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    PaymentEventProducer.class);

    private static final String PAYMENT_INITIATED_TOPIC =
            "payment-initiated";

    private static final String PAYMENT_STATUS_TOPIC =
            "payment-status";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentInitiated(
            String transactionId) {

        kafkaTemplate.send(
                PAYMENT_INITIATED_TOPIC,
                transactionId);

        logger.info(
                "Published Payment Initiated Event : {}",
                transactionId);
    }

    public void publishPaymentStatus(
            String transactionId,
            String status) {

        kafkaTemplate.send(
                PAYMENT_STATUS_TOPIC,
                transactionId + ":" + status);

        logger.info(
                "Published Payment Status Event : {} {}",
                transactionId,
                status);
    }
}