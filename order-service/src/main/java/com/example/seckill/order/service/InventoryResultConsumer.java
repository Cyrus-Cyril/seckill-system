package com.example.seckill.order.service;

import com.example.seckill.order.messaging.InventoryDeductResultMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryResultConsumer {

    private final OrderService orderService;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public InventoryResultConsumer(OrderService orderService, KafkaMessagePublisher kafkaMessagePublisher) {
        this.orderService = orderService;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @KafkaListener(topics = "${seckill.kafka.inventory-result-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        InventoryDeductResultMessage result = kafkaMessagePublisher.read(payload, InventoryDeductResultMessage.class);
        orderService.handleInventoryResult(result);
    }
}
