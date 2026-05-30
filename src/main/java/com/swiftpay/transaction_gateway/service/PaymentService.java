package com.swiftpay.transaction_gateway.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.kafka.PaymentEventProducer;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_IDEMPOTENCY_PREFIX = "payment:";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          RedisTemplate<String, String> redisTemplate,
                          PaymentEventProducer paymentEventProducer) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.paymentEventProducer = paymentEventProducer;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Transaction> getTransactionsByUser(String userId) {
        return transactionRepository.findBySenderIdOrReceiverId(userId, userId);
    }

    @Transactional
    public Transaction createPayment(Transaction request) {
        String transactionId = Optional.ofNullable(request.getTransactionId())
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());

        String redisKey = PAYMENT_IDEMPOTENCY_PREFIX + transactionId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            logger.warn("Duplicate payment request for transactionId={}", transactionId);
            Transaction duplicateResponse = new Transaction();
            duplicateResponse.setTransactionId(transactionId);
            duplicateResponse.setStatus("DUPLICATE");
            duplicateResponse.setCreatedAt(LocalDateTime.now());
            return duplicateResponse;
        }

        Account sender = accountRepository.findById(request.getSenderId()).orElse(null);
        if (sender != null && sender.getBalance() != null && sender.getBalance() < request.getAmount()) {
            logger.warn("Sender has insufficient balance for transactionId={}", transactionId);
            Transaction failedResponse = new Transaction();
            failedResponse.setTransactionId(transactionId);
            failedResponse.setStatus("FAILED");
            failedResponse.setSenderId(request.getSenderId());
            failedResponse.setReceiverId(request.getReceiverId());
            failedResponse.setAmount(request.getAmount());
            failedResponse.setCurrency(request.getCurrency());
            failedResponse.setCreatedAt(LocalDateTime.now());
            return failedResponse;
        }

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
        redisTemplate.opsForValue().set(redisKey, "PENDING", Duration.ofHours(24));

        paymentEventProducer.publishPaymentInitiated(transaction);

        logger.info("Payment queued for ledger processing transactionId={}", transactionId);
        return transaction;
    }

    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
