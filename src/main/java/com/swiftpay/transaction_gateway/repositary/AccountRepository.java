package com.swiftpay.transaction_gateway.repositary;

import org.springframework.data.jpa.repository.JpaRepository;
import com.swiftpay.transaction_gateway.entity.Account;

public interface AccountRepository
        extends JpaRepository<Account, String> {

}