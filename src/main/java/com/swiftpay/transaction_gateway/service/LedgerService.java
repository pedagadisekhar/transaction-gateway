package com.swiftpay.transaction_gateway.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.kafka.PaymentEventProducer;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            logger.debug("LedgerService skipped transactionId={} with status={}",
                    transaction.getTransactionId(),
                    transaction.getStatus());
            return;
        }

        String senderId = transaction.getSenderId();
        String receiverId = transaction.getReceiverId();
        List<String> orderedIds = senderId.compareTo(receiverId) <= 0
                ? List.of(senderId, receiverId)
                : List.of(receiverId, senderId);

        Map<String, Account> accountsById = accountRepository.findAllByIdInForUpdate(orderedIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        Account sender = accountsById.get(senderId);
        Account receiver = accountsById.get(receiverId);

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

        publishStatusAfterCommit(transaction.getTransactionId(), "COMPLETED");

        logger.debug("LedgerService completed payment for transactionId={}", transaction.getTransactionId());
    }

    private void completeTransactionWithFailure(Transaction transaction, String reason) {
        transaction.setStatus("FAILED");
        transactionRepository.save(transaction);

        publishStatusAfterCommit(transaction.getTransactionId(), "FAILED");

        logger.warn("LedgerService failed payment transactionId={} reason={}",
                transaction.getTransactionId(),
                reason);
    }

    private void publishStatusAfterCommit(String transactionId, String status) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentEventProducer.publishPaymentStatus(transactionId, status);
            }
        });
    }
}
