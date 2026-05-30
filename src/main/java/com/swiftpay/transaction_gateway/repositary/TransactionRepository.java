package com.swiftpay.transaction_gateway.repositary;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.swiftpay.transaction_gateway.entity.Transaction;

public interface TransactionRepository
        extends JpaRepository<Transaction, String> {

    List<Transaction> findBySenderIdOrReceiverId(String senderId, String receiverId);

    Optional<Transaction> findByTransactionId(String transactionId);
}
