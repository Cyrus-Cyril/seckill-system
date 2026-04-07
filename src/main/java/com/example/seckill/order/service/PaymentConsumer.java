package com.example.seckill.order.service;

import com.example.seckill.order.messaging.PaymentCommand;
import com.example.seckill.order.messaging.PaymentResultMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private final KafkaMessagePublisher kafkaMessagePublisher;
    private final String paymentResultTopic;

    public PaymentConsumer(KafkaMessagePublisher kafkaMessagePublisher,
                           @Value("${seckill.kafka.payment-result-topic}") String paymentResultTopic) {
        this.kafkaMessagePublisher = kafkaMessagePublisher;
        this.paymentResultTopic = paymentResultTopic;
    }

    @KafkaListener(topics = "${seckill.kafka.payment-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String payload) {
        PaymentCommand command = kafkaMessagePublisher.read(payload, PaymentCommand.class);

        PaymentResultMessage result = new PaymentResultMessage();
        result.setOrderId(command.getOrderId());
        result.setSuccess(true);
        result.setReason("PAY_SUCCESS");
        kafkaMessagePublisher.publish(paymentResultTopic, String.valueOf(command.getOrderId()), result);
    }
}
