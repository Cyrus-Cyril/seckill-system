package com.example.seckill.order.service;

import com.example.seckill.order.messaging.PaymentResultMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultConsumer {

    private final OrderService orderService;
    private final KafkaMessagePublisher kafkaMessagePublisher;

    public PaymentResultConsumer(OrderService orderService, KafkaMessagePublisher kafkaMessagePublisher) {
        this.orderService = orderService;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
    }

    @KafkaListener(topics = "${seckill.kafka.payment-result-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        PaymentResultMessage result = kafkaMessagePublisher.read(payload, PaymentResultMessage.class);
        orderService.handlePaymentResult(result);
    }
}
