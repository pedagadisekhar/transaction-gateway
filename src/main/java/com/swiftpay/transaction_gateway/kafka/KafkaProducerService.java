package com.swiftpay.transaction_gateway.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {

        kafkaTemplate.send(
                "payment-initiated",
                message);

        System.out.println(
                "Kafka Message Sent : " + message);
    }
}