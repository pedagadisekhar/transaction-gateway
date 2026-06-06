package com.swiftpay.transaction_gateway.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_IDEMPOTENCY_PREFIX = "payment:";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PaymentPersistenceService paymentPersistenceService;

    public PaymentService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          RedisTemplate<String, String> redisTemplate,
                          PaymentPersistenceService paymentPersistenceService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.paymentPersistenceService = paymentPersistenceService;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Transaction> getTransactionsByUser(String userId) {
        return transactionRepository.findBySenderIdOrReceiverId(userId, userId);
    }

    public Transaction createPayment(Transaction request) {
        String transactionId = Optional.ofNullable(request.getTransactionId())
                .filter(id -> !id.isBlank())
                .orElse(UUID.randomUUID().toString());

        String redisKey = PAYMENT_IDEMPOTENCY_PREFIX + transactionId;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(redisKey, "PENDING", Duration.ofHours(24));
        if (Boolean.FALSE.equals(created)) {
            logger.warn("Duplicate payment request for transactionId={}", transactionId);
            Transaction duplicateResponse = new Transaction();
            duplicateResponse.setTransactionId(transactionId);
            duplicateResponse.setStatus("DUPLICATE");
            duplicateResponse.setCreatedAt(LocalDateTime.now());
            return duplicateResponse;
        }

        try {
            return paymentPersistenceService.savePendingPayment(request, transactionId);
        } catch (Exception ex) {
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }

    public Transaction getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
