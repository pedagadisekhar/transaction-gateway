package com.swiftpay.transaction_gateway.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTestController(
            RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/redis-test")
    public String testRedis() {

        redisTemplate.opsForValue()
        .set("test", "swiftpay");

        return redisTemplate.opsForValue()
                .get("test");
    }
}