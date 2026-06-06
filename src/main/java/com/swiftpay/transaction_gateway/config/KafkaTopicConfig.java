package com.swiftpay.transaction_gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name("payment-initiated")
                .partitions(16)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentStatusTopic() {
        return TopicBuilder.name("payment-status")
                .partitions(8)
                .replicas(1)
                .build();
    }
}
