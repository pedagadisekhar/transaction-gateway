package com.swiftpay.transaction_gateway.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.service.LedgerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LedgerEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEventConsumer.class);

    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    public LedgerEventConsumer(LedgerService ledgerService,
                               ObjectMapper objectMapper) {
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-initiated", groupId = "ledger-service-group")
    public void consume(String message) {
        try {
            Transaction transaction = objectMapper.readValue(message, Transaction.class);
            ledgerService.handlePaymentInitiated(transaction);
        } catch (Exception ex) {
            logger.error("Failed to handle payment-initiated event", ex);
        }
    }
}
