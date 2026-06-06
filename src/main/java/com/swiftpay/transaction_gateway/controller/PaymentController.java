package com.swiftpay.transaction_gateway.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.entity.Transaction;
import com.swiftpay.transaction_gateway.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private static final Logger logger =
            LoggerFactory.getLogger(PaymentController.class);

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return paymentService.getAllAccounts();
    }

    @PostMapping("/v1/payments")
    public Transaction createPayment(@RequestBody Transaction transaction) {
        logger.debug("Payment request received for senderId={}", transaction.getSenderId());
        return paymentService.createPayment(transaction);
    }

    @GetMapping("/v1/payments/user/{userId}")
    public List<Transaction> getTransactions(@PathVariable String userId) {
        return paymentService.getTransactionsByUser(userId);
    }

    @GetMapping("/v1/payments/{transactionId}")
    public Transaction getTransactionById(@PathVariable String transactionId) {
        return paymentService.getTransactionById(transactionId);
    }
}
