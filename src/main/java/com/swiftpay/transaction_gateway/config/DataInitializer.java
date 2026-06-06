package com.swiftpay.transaction_gateway.config;

import com.swiftpay.transaction_gateway.entity.Account;
import com.swiftpay.transaction_gateway.repositary.AccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private static final String LOAD_TEST_SENDER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String LOAD_TEST_RECEIVER_ID = "22222222-2222-2222-2222-222222222222";

    private final AccountRepository accountRepository;

    public DataInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAccount(LOAD_TEST_SENDER_ID, "load-test-sender", 100_000_000.0);
        seedAccount(LOAD_TEST_RECEIVER_ID, "load-test-receiver", 0.0);
    }

    private void seedAccount(String id, String userName, double balance) {
        accountRepository.findById(id).ifPresentOrElse(
                existing -> {
                    if (existing.getBalance() == null || existing.getBalance() < 10_000_000.0) {
                        existing.setBalance(100_000_000.0);
                        accountRepository.save(existing);
                        logger.info("Replenished load-test account id={}", id);
                    }
                },
                () -> {
                    Account account = new Account();
                    account.setId(id);
                    account.setUserName(userName);
                    account.setBalance(balance);
                    accountRepository.save(account);
                    logger.info("Seeded load-test account id={} balance={}", id, balance);
                });
    }
}
