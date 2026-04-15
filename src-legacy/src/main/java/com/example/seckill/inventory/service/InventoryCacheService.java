package com.example.seckill.inventory.service;

import com.example.seckill.inventory.entity.Inventory;
import com.example.seckill.inventory.mapper.InventoryMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryCacheService {

    private final InventoryMapper inventoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final String stockPrefix;

    public InventoryCacheService(InventoryMapper inventoryMapper,
                                 StringRedisTemplate stringRedisTemplate,
                                 @Value("${seckill.redis.stock-prefix}") String stockPrefix) {
        this.inventoryMapper = inventoryMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.stockPrefix = stockPrefix;
    }

    @PostConstruct
    public void preloadStock() {
        List<Inventory> inventories = inventoryMapper.findAll();
        for (Inventory inventory : inventories) {
            stringRedisTemplate.opsForValue().set(
                    stockPrefix + inventory.getProductId(),
                    String.valueOf(inventory.getAvailableStock())
            );
        }
    }
}
