package com.swiftpay.transaction_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

	    @Bean
	    public LettuceConnectionFactory redisConnectionFactory() {
	        return new LettuceConnectionFactory(
	                "localhost",
	                6379);
	    }

	    @Bean
	    public RedisTemplate<String, String> redisTemplate() {

	        RedisTemplate<String, String> template =
	                new RedisTemplate<>();

	        template.setConnectionFactory(
	                redisConnectionFactory());

	        template.afterPropertiesSet();

	        return template;
	    }
}