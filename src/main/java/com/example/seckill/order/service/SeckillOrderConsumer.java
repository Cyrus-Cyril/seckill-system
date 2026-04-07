package com.example.seckill.order.service;

import com.example.seckill.order.messaging.OrderCreateCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public SeckillOrderConsumer(OrderService orderService, KafkaMessagePublisher kafkaMessagePublisher) {
        this.orderService = orderService;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @KafkaListener(topics = "${seckill.kafka.order-create-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        OrderCreateCommand command = kafkaMessagePublisher.read(payload, OrderCreateCommand.class);
        orderService.createPendingOrder(command);
    }
}
