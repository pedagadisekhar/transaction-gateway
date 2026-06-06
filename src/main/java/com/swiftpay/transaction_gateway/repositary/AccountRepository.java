package com.swiftpay.transaction_gateway.repositary;

import java.util.List;

import jakarta.persistence.LockModeType;

import com.swiftpay.transaction_gateway.entity.Account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN :ids ORDER BY a.id")
    List<Account> findAllByIdInForUpdate(@Param("ids") List<String> ids);
}
