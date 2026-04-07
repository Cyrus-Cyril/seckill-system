package com.example.seckill.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillRedisService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('exists', KEYS[2]) == 1 then
                return 2
            end
            local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
            if stock < tonumber(ARGV[1]) then
                return 0
            end
            redis.call('decrby', KEYS[1], ARGV[1])
            redis.call('set', KEYS[2], ARGV[2], 'EX', ARGV[3])
            return 1
            """,
            Long.class
    );

    private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('get', KEYS[2])
            if current == ARGV[2] then
                redis.call('incrby', KEYS[1], ARGV[1])
                redis.call('del', KEYS[2])
                return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final String stockPrefix;
    private final String userOrderPrefix;

    public SeckillRedisService(StringRedisTemplate stringRedisTemplate,
                               @Value("${seckill.redis.stock-prefix}") String stockPrefix,
                               @Value("${seckill.redis.user-order-prefix}") String userOrderPrefix) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.stockPrefix = stockPrefix;
        this.userOrderPrefix = userOrderPrefix;
    }

    public ReserveResult reserve(Long productId, Long userId, Long orderId, Integer quantity) {
        Long result = stringRedisTemplate.execute(
                RESERVE_SCRIPT,
                List.of(stockPrefix + productId, buildUserOrderKey(userId, productId)),
                String.valueOf(quantity),
                String.valueOf(orderId),
                String.valueOf(TimeUnit.MINUTES.toSeconds(30))
        );
        if (Long.valueOf(1L).equals(result)) {
            return ReserveResult.SUCCESS;
        }
        if (Long.valueOf(2L).equals(result)) {
            return ReserveResult.DUPLICATE;
        }
        return ReserveResult.SOLD_OUT;
    }

    public void markOrderCreated(Long userId, Long productId, Long orderId) {
        stringRedisTemplate.opsForValue().set(
                buildUserOrderKey(userId, productId),
                "SUCCESS:" + orderId,
                7,
                TimeUnit.DAYS
        );
    }

    public void rollbackReservation(Long userId, Long productId, Long orderId, Integer quantity) {
        stringRedisTemplate.execute(
                ROLLBACK_SCRIPT,
                List.of(stockPrefix + productId, buildUserOrderKey(userId, productId)),
                String.valueOf(quantity),
                String.valueOf(orderId)
        );
    }

    private String buildUserOrderKey(Long userId, Long productId) {
        return userOrderPrefix + userId + ":" + productId;
    }

    public enum ReserveResult {
        SUCCESS,
        SOLD_OUT,
        DUPLICATE
    }
}
