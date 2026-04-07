package com.example.seckill.inventory.service;

import com.example.seckill.order.messaging.InventoryDeductCommand;
import com.example.seckill.order.service.KafkaMessagePublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryDeductConsumer {

    private final InventoryService inventoryService;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public InventoryDeductConsumer(InventoryService inventoryService, KafkaMessagePublisher kafkaMessagePublisher) {
        this.inventoryService = inventoryService;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @KafkaListener(topics = "${seckill.kafka.inventory-deduct-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        InventoryDeductCommand command = kafkaMessagePublisher.read(payload, InventoryDeductCommand.class);
        inventoryService.deduct(command);
    }
}
