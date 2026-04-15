package com.example.seckill.order.service;

import com.example.seckill.common.BusinessException;
import com.example.seckill.common.SnowflakeIdGenerator;
import com.example.seckill.order.dto.PaymentSubmitResponse;
import com.example.seckill.order.dto.SeckillOrderRequest;
import com.example.seckill.order.dto.SeckillSubmitResponse;
import com.example.seckill.order.entity.Order;
import com.example.seckill.order.entity.OrderStatus;
import com.example.seckill.order.entity.SeckillOrderRecord;
import com.example.seckill.order.mapper.OrderMapper;
import com.example.seckill.order.mapper.SeckillOrderRecordMapper;
import com.example.seckill.order.messaging.InventoryDeductCommand;
import com.example.seckill.order.messaging.InventoryDeductResultMessage;
import com.example.seckill.order.messaging.OrderCreateCommand;
import com.example.seckill.order.messaging.PaymentCommand;
import com.example.seckill.order.messaging.PaymentResultMessage;
import com.example.seckill.product.entity.Product;
import com.example.seckill.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final int ORDER_TYPE_SECKILL = 1;

    private final OrderMapper orderMapper;
    private final SeckillOrderRecordMapper seckillOrderRecordMapper;
    private final ProductMapper productMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final SeckillRedisService seckillRedisService;
    private final KafkaMessagePublisher kafkaMessagePublisher;
    private final String orderCreateTopic;
    private final String inventoryDeductTopic;
    private final String paymentTopic;

    public OrderService(OrderMapper orderMapper,
                        SeckillOrderRecordMapper seckillOrderRecordMapper,
                        ProductMapper productMapper,
                        SnowflakeIdGenerator snowflakeIdGenerator,
                        SeckillRedisService seckillRedisService,
                        KafkaMessagePublisher kafkaMessagePublisher,
                        @Value("${seckill.kafka.order-create-topic}") String orderCreateTopic,
                        @Value("${seckill.kafka.inventory-deduct-topic}") String inventoryDeductTopic,
                        @Value("${seckill.kafka.payment-topic}") String paymentTopic) {
        this.orderMapper = orderMapper;
        this.seckillOrderRecordMapper = seckillOrderRecordMapper;
        this.productMapper = productMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.seckillRedisService = seckillRedisService;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
        this.orderCreateTopic = orderCreateTopic;
        this.inventoryDeductTopic = inventoryDeductTopic;
        this.paymentTopic = paymentTopic;
    }

    public SeckillSubmitResponse submitSeckillOrder(SeckillOrderRequest request) {
        if (request.getQuantity() == null || request.getQuantity() != 1) {
            throw new BusinessException("秒杀场景暂只支持单件购买");
        }

        Product product = productMapper.findById(request.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException("商品不存在或不可购买");
        }

        SeckillOrderRecord existing = seckillOrderRecordMapper.findByUserIdAndProductId(
                request.getUserId(),
                request.getProductId()
        );
        if (existing != null) {
            throw new BusinessException("同一用户同一商品只能秒杀一次");
        }

        long orderId = snowflakeIdGenerator.nextId();
        SeckillRedisService.ReserveResult reserveResult = seckillRedisService.reserve(
                request.getProductId(),
                request.getUserId(),
                orderId,
                request.getQuantity()
        );

        if (reserveResult == SeckillRedisService.ReserveResult.DUPLICATE) {
            throw new BusinessException("请勿重复下单");
        }
        if (reserveResult == SeckillRedisService.ReserveResult.SOLD_OUT) {
            throw new BusinessException("库存不足");
        }

        OrderCreateCommand command = new OrderCreateCommand();
        command.setOrderId(orderId);
        command.setUserId(request.getUserId());
        command.setProductId(request.getProductId());
        command.setQuantity(request.getQuantity());
        kafkaMessagePublisher.publish(orderCreateTopic, String.valueOf(request.getProductId()), command);

        return new SeckillSubmitResponse(orderId, "PROCESSING", "秒杀请求已受理，正在异步创建订单");
    }

    public PaymentSubmitResponse payOrder(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("当前订单状态不支持支付");
        }

        PaymentCommand command = new PaymentCommand();
        command.setOrderId(order.getId());
        command.setUserId(order.getUserId());
        command.setAmount(order.getOrderAmount());
        kafkaMessagePublisher.publish(paymentTopic, String.valueOf(order.getId()), command);

        return new PaymentSubmitResponse(orderId, "PROCESSING", "支付请求已受理，正在异步处理");
    }

    public Order getOrderById(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    public Order getOrderByOrderNo(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Transactional
    public void createPendingOrder(OrderCreateCommand command) {
        if (orderMapper.findById(command.getOrderId()) != null) {
            return;
        }

        Product product = productMapper.findById(command.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            seckillRedisService.rollbackReservation(
                    command.getUserId(),
                    command.getProductId(),
                    command.getOrderId(),
                    command.getQuantity()
            );
            return;
        }

        Order order = new Order();
        order.setId(command.getOrderId());
        order.setOrderNo(String.valueOf(command.getOrderId()));
        order.setUserId(command.getUserId());
        order.setProductId(command.getProductId());
        order.setQuantity(command.getQuantity());
        order.setOrderAmount(product.getPrice().multiply(BigDecimal.valueOf(command.getQuantity())));
        order.setStatus(OrderStatus.PENDING_STOCK);
        order.setOrderType(ORDER_TYPE_SECKILL);

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            return;
        }

        InventoryDeductCommand deductCommand = new InventoryDeductCommand();
        deductCommand.setOrderId(command.getOrderId());
        deductCommand.setUserId(command.getUserId());
        deductCommand.setProductId(command.getProductId());
        deductCommand.setQuantity(command.getQuantity());
        kafkaMessagePublisher.publish(inventoryDeductTopic, String.valueOf(command.getProductId()), deductCommand);
    }

    @Transactional
    public void handleInventoryResult(InventoryDeductResultMessage message) {
        Order order = orderMapper.findById(message.getOrderId());
        if (order == null) {
            return;
        }

        if (order.getStatus() != null && order.getStatus() == OrderStatus.PAID) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == OrderStatus.PENDING_PAYMENT && Boolean.TRUE.equals(message.getSuccess())) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() == OrderStatus.FAILED && Boolean.FALSE.equals(message.getSuccess())) {
            return;
        }

        if (Boolean.TRUE.equals(message.getSuccess())) {
            orderMapper.updateStatus(order.getId(), OrderStatus.PENDING_STOCK, OrderStatus.PENDING_PAYMENT);
            try {
                SeckillOrderRecord record = new SeckillOrderRecord();
                record.setUserId(order.getUserId());
                record.setProductId(order.getProductId());
                record.setOrderId(order.getId());
                seckillOrderRecordMapper.insert(record);
            } catch (DuplicateKeyException ignored) {
            }
            seckillRedisService.markOrderCreated(order.getUserId(), order.getProductId(), order.getId());
            return;
        }

        orderMapper.forceUpdateStatus(order.getId(), OrderStatus.FAILED);
        seckillRedisService.rollbackReservation(
                message.getUserId(),
                message.getProductId(),
                message.getOrderId(),
                message.getQuantity()
        );
    }

    @Transactional
    public void handlePaymentResult(PaymentResultMessage message) {
        Order order = orderMapper.findById(message.getOrderId());
        if (order == null) {
            return;
        }

        if (order.getStatus() != null && order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if (Boolean.TRUE.equals(message.getSuccess())) {
            orderMapper.updateStatus(order.getId(), OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
            return;
        }

        orderMapper.forceUpdateStatus(order.getId(), OrderStatus.PENDING_PAYMENT);
    }
}
