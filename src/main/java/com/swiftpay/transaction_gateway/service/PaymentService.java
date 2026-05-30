package com.swiftpay.transaction_gateway.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.kafka.KafkaProducerService;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;
import com.swiftpay.transaction_gateway.repositary.TransactionRepository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {

        private final AccountRepository accountRepository;
        private final TransactionRepository transactionRepository;
        private final RedisTemplate<String, String> redisTemplate;
        private final KafkaProducerService kafkaProducerService;

        private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

        public PaymentService(AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        RedisTemplate<String, String> redisTemplate, KafkaProducerService kafkaProducerService) {

                this.accountRepository = accountRepository;
                this.transactionRepository = transactionRepository;
                this.redisTemplate = redisTemplate;
                this.kafkaProducerService = kafkaProducerService;
        }

        public List<Account> getAllAccounts() {
                return accountRepository.findAll();
        }

        public List<Transaction> getTransactionsByUser(
                        String userId) {

                return transactionRepository
                                .findBySenderIdOrReceiverId(
                                                userId,
                                                userId);
        }

        @Transactional
        public String createPayment(Transaction transaction) {

                String redisKey = "payment:" +
                                transaction.getTransactionId();

                if (Boolean.TRUE.equals(
                                redisTemplate.hasKey(redisKey))) {

                        logger.warn(
                                        "Duplicate Transaction : {}",
                                        transaction.getTransactionId());

                        return "Duplicate Transaction";
                }

                transaction.setId(
                                UUID.randomUUID().toString());

                transaction.setStatus("PENDING");

                transaction.setCreatedAt(
                                LocalDateTime.now());

                transactionRepository.save(transaction);

                redisTemplate.opsForValue().set(
                                redisKey,
                                "PROCESSED",
                                java.time.Duration.ofHours(24));

                kafkaProducerService.sendMessage(
                                transaction.getId());

                logger.info(
                                "Payment Submitted To Kafka");

                return "Payment Submitted";
        }

        @Transactional
        public void processPayment(
                        String transactionId) {

                Transaction transaction = transactionRepository
                                .findById(transactionId)
                                .orElse(null);

                if (transaction == null) {
                        return;
                }

                Account sender = accountRepository
                                .findById(
                                                transaction.getSenderId())
                                .orElse(null);

                Account receiver = accountRepository
                                .findById(
                                                transaction.getReceiverId())
                                .orElse(null);

                if (sender == null || receiver == null) {

                        transaction.setStatus("FAILED");

                        transactionRepository.save(
                                        transaction);

                        return;
                }

                if (sender.getBalance() < transaction.getAmount()) {

                        transaction.setStatus("FAILED");

                        transactionRepository.save(
                                        transaction);

                        return;
                }

                sender.setBalance(
                                sender.getBalance()
                                                - transaction.getAmount());

                receiver.setBalance(
                                receiver.getBalance()
                                                + transaction.getAmount());

                accountRepository.save(sender);

                accountRepository.save(receiver);

                transaction.setStatus("COMPLETED");

                transactionRepository.save(
                                transaction);

                logger.info(
                                "Payment Completed Through Kafka");
        }

        public Transaction getTransactionById(String transactionId) {

                return transactionRepository
                                .findById(transactionId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Transaction Not Found"));
        }

}
