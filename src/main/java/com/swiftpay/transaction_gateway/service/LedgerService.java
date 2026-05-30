package com.swiftpay.transaction_gateway.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.kafka.PaymentEventProducer;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    public LedgerService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository,
                         PaymentEventProducer paymentEventProducer) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public void handlePaymentInitiated(Transaction event) {
        Transaction transaction = transactionRepository
                .findByTransactionId(event.getTransactionId())
                .orElse(null);

        if (transaction == null) {
            logger.warn("LedgerService skipped unknown transactionId={}", event.getTransactionId());
            return;
        }

        if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
            logger.info("LedgerService skipped transactionId={} with status={}",
                    transaction.getTransactionId(),
                    transaction.getStatus());
            return;
        }

        Account sender = accountRepository
                .findById(event.getSenderId())
                .orElse(null);
        Account receiver = accountRepository
                .findById(event.getReceiverId())
                .orElse(null);

        if (sender == null || receiver == null) {
            completeTransactionWithFailure(transaction, "Sender or receiver account not found");
            return;
        }

        if (sender.getBalance() == null || sender.getBalance() < event.getAmount()) {
            completeTransactionWithFailure(transaction, "Insufficient funds");
            return;
        }

        sender.setBalance(sender.getBalance() - event.getAmount());
        receiver.setBalance(receiver.getBalance() + event.getAmount());

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("transactionId", transaction.getTransactionId());
        statusPayload.put("status", "COMPLETED");
        statusPayload.put("reason", "Payment completed successfully");
        statusPayload.put("completedAt", LocalDateTime.now().toString());

        paymentEventProducer.publishPaymentStatus(transaction.getTransactionId(), statusPayload);

        logger.info("LedgerService completed payment for transactionId={}", transaction.getTransactionId());
    }

    private void completeTransactionWithFailure(Transaction transaction, String reason) {
        transaction.setStatus("FAILED");
        transactionRepository.save(transaction);

        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("transactionId", transaction.getTransactionId());
        statusPayload.put("status", "FAILED");
        statusPayload.put("reason", reason);
        statusPayload.put("completedAt", LocalDateTime.now().toString());

        paymentEventProducer.publishPaymentStatus(transaction.getTransactionId(), statusPayload);

        logger.warn("LedgerService failed payment transactionId={} reason= {}",
                transaction.getTransactionId(),
                reason);
    }
}
