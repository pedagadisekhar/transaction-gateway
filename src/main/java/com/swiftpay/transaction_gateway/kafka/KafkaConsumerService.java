package com.swiftpay.transaction_gateway.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.swiftpay.transaction_gateway.service.PaymentService;

@Service
public class KafkaConsumerService {

    private final PaymentService paymentService;

    public KafkaConsumerService(
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "payment-initiated", groupId = "payment-group")
    public void consume(String transactionId) {

        paymentService.processPayment(
                transactionId);
    }

}