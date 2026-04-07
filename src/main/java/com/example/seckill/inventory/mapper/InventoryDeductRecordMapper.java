package com.example.seckill.inventory.mapper;

import com.example.seckill.inventory.entity.InventoryDeductRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryDeductRecordMapper {

    @Select("""
        SELECT id, order_id, product_id, quantity, status, create_time, update_time
        FROM tb_inventory_deduct_record
        WHERE order_id = #{orderId}
        LIMIT 1
        """)
    InventoryDeductRecord findByOrderId(Long orderId);

    @Insert("""
        INSERT INTO tb_inventory_deduct_record(order_id, product_id, quantity, status)
        VALUES(#{orderId}, #{productId}, #{quantity}, #{status})
        """)
    int insert(InventoryDeductRecord record);
}
