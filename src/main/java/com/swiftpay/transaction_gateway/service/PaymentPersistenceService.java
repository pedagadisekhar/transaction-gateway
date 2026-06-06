package com.swiftpay.transaction_gateway.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.kafka.PaymentEventProducer;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PaymentPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentPersistenceService.class);

    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentPersistenceService(TransactionRepository transactionRepository,
                                       PaymentEventProducer paymentEventProducer) {
        this.transactionRepository = transactionRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public Transaction savePendingPayment(Transaction request, String transactionId) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setTransactionId(transactionId);
        transaction.setSenderId(request.getSenderId());
        transaction.setReceiverId(request.getReceiverId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentEventProducer.publishPaymentInitiated(transaction.getTransactionId());
            }
        });

        logger.debug("Payment queued for ledger processing transactionId={}", transactionId);
        return transaction;
    }
}
