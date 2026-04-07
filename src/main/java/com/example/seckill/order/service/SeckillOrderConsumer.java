package com.example.seckill.order.service;

import com.example.seckill.order.messaging.SeckillOrderMessage;
import com.example.seckill.product.entity.Product;
import com.example.seckill.product.mapper.ProductMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final ProductMapper productMapper;

    public SeckillOrderConsumer(OrderService orderService,
                                ProductMapper productMapper) {
        this.orderService = orderService;
        this.productMapper = productMapper;
    }

    @KafkaListener(topics = "${seckill.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(SeckillOrderMessage message) {
        Product product = productMapper.findById(message.getProductId());
        if (product == null) {
            orderService.rollbackReservation(
                    message.getUserId(),
                    message.getProductId(),
                    message.getOrderId(),
                    message.getQuantity()
            );
            return;
        }

        OrderService.ProcessResult result = orderService.createSeckillOrder(message, product);
        if (result == OrderService.ProcessResult.OUT_OF_STOCK) {
            orderService.rollbackReservation(
                    message.getUserId(),
                    message.getProductId(),
                    message.getOrderId(),
                    message.getQuantity()
            );
            return;
        }

        orderService.markOrderCreated(message.getUserId(), message.getProductId(), message.getOrderId());
    }
}
