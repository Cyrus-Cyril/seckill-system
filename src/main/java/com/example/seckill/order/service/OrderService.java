package com.example.seckill.order.service;

import com.example.seckill.common.BusinessException;
import com.example.seckill.common.SnowflakeIdGenerator;
import com.example.seckill.inventory.mapper.InventoryMapper;
import com.example.seckill.order.dto.SeckillOrderRequest;
import com.example.seckill.order.dto.SeckillSubmitResponse;
import com.example.seckill.order.entity.Order;
import com.example.seckill.order.entity.SeckillOrderRecord;
import com.example.seckill.order.mapper.OrderMapper;
import com.example.seckill.order.mapper.SeckillOrderRecordMapper;
import com.example.seckill.order.messaging.SeckillOrderMessage;
import com.example.seckill.product.entity.Product;
import com.example.seckill.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final int ORDER_TYPE_SECKILL = 1;
    private static final int ORDER_STATUS_CREATED = 1;

    private final OrderMapper orderMapper;
    private final SeckillOrderRecordMapper seckillOrderRecordMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final KafkaTemplate<String, SeckillOrderMessage> kafkaTemplate;
    private final SeckillRedisService seckillRedisService;
    private final String seckillTopic;

    public OrderService(OrderMapper orderMapper,
                        SeckillOrderRecordMapper seckillOrderRecordMapper,
                        ProductMapper productMapper,
                        InventoryMapper inventoryMapper,
                        SnowflakeIdGenerator snowflakeIdGenerator,
                        KafkaTemplate<String, SeckillOrderMessage> kafkaTemplate,
                        SeckillRedisService seckillRedisService,
                        @Value("${seckill.kafka.topic}") String seckillTopic) {
        this.orderMapper = orderMapper;
        this.seckillOrderRecordMapper = seckillOrderRecordMapper;
        this.productMapper = productMapper;
        this.inventoryMapper = inventoryMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.seckillRedisService = seckillRedisService;
        this.seckillTopic = seckillTopic;
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

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setOrderId(orderId);
        message.setUserId(request.getUserId());
        message.setProductId(request.getProductId());
        message.setQuantity(request.getQuantity());
        kafkaTemplate.send(seckillTopic, String.valueOf(request.getProductId()), message);

        return new SeckillSubmitResponse(orderId, "PROCESSING", "秒杀请求已受理，正在异步创建订单");
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
    public ProcessResult createSeckillOrder(SeckillOrderMessage message, Product product) {
        if (orderMapper.findById(message.getOrderId()) != null) {
            return ProcessResult.ALREADY_CREATED;
        }

        SeckillOrderRecord existing = seckillOrderRecordMapper.findByUserIdAndProductId(
                message.getUserId(),
                message.getProductId()
        );
        if (existing != null) {
            return ProcessResult.ALREADY_CREATED;
        }

        int deductedRows = inventoryMapper.deductAvailableStock(message.getProductId(), message.getQuantity());
        if (deductedRows <= 0) {
            return ProcessResult.OUT_OF_STOCK;
        }

        Order order = new Order();
        order.setId(message.getOrderId());
        order.setOrderNo(String.valueOf(message.getOrderId()));
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setQuantity(message.getQuantity());
        order.setOrderAmount(product.getPrice().multiply(BigDecimal.valueOf(message.getQuantity())));
        order.setStatus(ORDER_STATUS_CREATED);
        order.setOrderType(ORDER_TYPE_SECKILL);

        SeckillOrderRecord record = new SeckillOrderRecord();
        record.setUserId(message.getUserId());
        record.setProductId(message.getProductId());
        record.setOrderId(message.getOrderId());

        try {
            orderMapper.insert(order);
            seckillOrderRecordMapper.insert(record);
            return ProcessResult.CREATED;
        } catch (DuplicateKeyException e) {
            return ProcessResult.ALREADY_CREATED;
        }
    }

    public void markOrderCreated(Long userId, Long productId, Long orderId) {
        seckillRedisService.markOrderCreated(userId, productId, orderId);
    }

    public void rollbackReservation(Long userId, Long productId, Long orderId, Integer quantity) {
        seckillRedisService.rollbackReservation(userId, productId, orderId, quantity);
    }

    public enum ProcessResult {
        CREATED,
        ALREADY_CREATED,
        OUT_OF_STOCK
    }
}
