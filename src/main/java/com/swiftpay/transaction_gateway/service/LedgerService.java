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
    public void handlePaymentInitiated(String transactionId) {
        Transaction transaction = transactionRepository
                .findByTransactionId(transactionId)
                .orElse(null);

        if (transaction == null) {
            logger.warn("LedgerService skipped unknown transactionId={}", transactionId);
            return;
        }

        if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
            logger.info("LedgerService skipped transactionId={} with status={}",
                    transaction.getTransactionId(),
                    transaction.getStatus());
            return;
        }

        Account sender = accountRepository
                .findById(transaction.getSenderId())
                .orElse(null);
        Account receiver = accountRepository
                .findById(transaction.getReceiverId())
                .orElse(null);

        if (sender == null || receiver == null) {
            completeTransactionWithFailure(transaction, "Sender or receiver account not found");
            return;
        }

        if (sender.getBalance() == null || sender.getBalance() < transaction.getAmount()) {
            completeTransactionWithFailure(transaction, "Insufficient funds");
            return;
        }

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        receiver.setBalance(receiver.getBalance() + transaction.getAmount());

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transaction.setStatus("COMPLETED");
        transactionRepository.save(transaction);

        paymentEventProducer.publishPaymentStatus(transaction.getTransactionId(), "COMPLETED");

        logger.info("LedgerService completed payment for transactionId={}", transaction.getTransactionId());
    }

    private void completeTransactionWithFailure(Transaction transaction, String reason) {
        transaction.setStatus("FAILED");
        transactionRepository.save(transaction);

        paymentEventProducer.publishPaymentStatus(transaction.getTransactionId(), "FAILED");

        logger.warn("LedgerService failed payment transactionId={} reason= {}",
                transaction.getTransactionId(),
                reason);
    }
}
