package com.example.seckill.product.service;

import com.example.seckill.product.entity.Product;
import com.example.seckill.product.mapper.ProductMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(ProductMapper productMapper,
                          StringRedisTemplate stringRedisTemplate,
                          ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Product getById(Long id) {
        String cacheKey = "product:detail:" + id;
        String lockKey = "product:lock:" + id;

        String json = stringRedisTemplate.opsForValue().get(cacheKey);

        if (json != null) {
            if ("null".equals(json)) {
                return null;
            }
            try {
                return objectMapper.readValue(json, Product.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("缓存反序列化失败");
            }
        }

        Boolean lockSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(lockSuccess)) {
            try {
                String secondJson = stringRedisTemplate.opsForValue().get(cacheKey);
                if (secondJson != null) {
                    if ("null".equals(secondJson)) {
                        return null;
                    }
                    return objectMapper.readValue(secondJson, Product.class);
                }

                Product product = productMapper.findById(id);

                if (product == null) {
                    stringRedisTemplate.opsForValue().set(
                            cacheKey,
                            "null",
                            Duration.ofMinutes(2).plusSeconds(ThreadLocalRandom.current().nextInt(30))
                    );
                    return null;
                }

                String productJson = objectMapper.writeValueAsString(product);
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        productJson,
                        Duration.ofMinutes(30).plusSeconds(ThreadLocalRandom.current().nextInt(300))
                );
                return product;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("缓存序列化失败");
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String retryJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (retryJson == null || "null".equals(retryJson)) {
            return null;
        }

        try {
            return objectMapper.readValue(retryJson, Product.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("缓存反序列化失败");
        }
    }
}