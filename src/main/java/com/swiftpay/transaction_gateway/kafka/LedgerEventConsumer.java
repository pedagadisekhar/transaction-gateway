package com.swiftpay.transaction_gateway.kafka;

import com.swiftpay.transaction_gateway.service.LedgerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LedgerEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEventConsumer.class);

    private final LedgerService ledgerService;

    public LedgerEventConsumer(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @KafkaListener(
            topics = "payment-initiated",
            groupId = "ledger-service-group",
            concurrency = "16")
    public void consume(String transactionId) {
        logger.debug("Received transactionId={}", transactionId);
        ledgerService.handlePaymentInitiated(transactionId);
    }
}
