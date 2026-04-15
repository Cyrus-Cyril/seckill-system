package com.example.seckill.inventory.service;

import com.example.seckill.inventory.entity.InventoryDeductRecord;
import com.example.seckill.inventory.mapper.InventoryDeductRecordMapper;
import com.example.seckill.inventory.mapper.InventoryMapper;
import com.example.seckill.order.messaging.InventoryDeductCommand;
import com.example.seckill.order.messaging.InventoryDeductResultMessage;
import com.example.seckill.order.service.KafkaMessagePublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final int DEDUCT_SUCCESS = 1;
    private static final int DEDUCT_FAILED = 2;

    private final InventoryMapper inventoryMapper;
    private final InventoryDeductRecordMapper inventoryDeductRecordMapper;
    private final KafkaMessagePublisher kafkaMessagePublisher;
    private final String inventoryResultTopic;

    public InventoryService(InventoryMapper inventoryMapper,
                            InventoryDeductRecordMapper inventoryDeductRecordMapper,
                            KafkaMessagePublisher kafkaMessagePublisher,
                            @Value("${seckill.kafka.inventory-result-topic}") String inventoryResultTopic) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryDeductRecordMapper = inventoryDeductRecordMapper;
        this.kafkaMessagePublisher = kafkaMessagePublisher;
        this.inventoryResultTopic = inventoryResultTopic;
    }

    @Transactional
    public void deduct(InventoryDeductCommand command) {
        InventoryDeductRecord existing = inventoryDeductRecordMapper.findByOrderId(command.getOrderId());
        if (existing != null) {
            publishResult(
                    command,
                    existing.getStatus() != null && existing.getStatus() == DEDUCT_SUCCESS,
                    existing.getStatus() != null && existing.getStatus() == DEDUCT_SUCCESS ? "OK" : "INSUFFICIENT_STOCK"
            );
            return;
        }

        int rows = inventoryMapper.deductAvailableStock(command.getProductId(), command.getQuantity());
        InventoryDeductRecord record = new InventoryDeductRecord();
        record.setOrderId(command.getOrderId());
        record.setProductId(command.getProductId());
        record.setQuantity(command.getQuantity());
        record.setStatus(rows > 0 ? DEDUCT_SUCCESS : DEDUCT_FAILED);
        try {
            inventoryDeductRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
        }

        publishResult(command, rows > 0, rows > 0 ? "OK" : "INSUFFICIENT_STOCK");
    }

    private void publishResult(InventoryDeductCommand command, boolean success, String reason) {
        InventoryDeductResultMessage resultMessage = new InventoryDeductResultMessage();
        resultMessage.setOrderId(command.getOrderId());
        resultMessage.setUserId(command.getUserId());
        resultMessage.setProductId(command.getProductId());
        resultMessage.setQuantity(command.getQuantity());
        resultMessage.setSuccess(success);
        resultMessage.setReason(reason);
        kafkaMessagePublisher.publish(inventoryResultTopic, String.valueOf(command.getOrderId()), resultMessage);
    }
}
