package com.example.seckill.inventory.mapper;

import com.example.seckill.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface InventoryMapper {

    @Select("""
        SELECT id, product_id, total_stock, available_stock, locked_stock, version, update_time
        FROM tb_inventory
        WHERE product_id = #{productId}
        LIMIT 1
        """)
    Inventory findByProductId(Long productId);

    @Select("""
        SELECT id, product_id, total_stock, available_stock, locked_stock, version, update_time
        FROM tb_inventory
        """)
    List<Inventory> findAll();

    @Update("""
        UPDATE tb_inventory
        SET available_stock = available_stock - #{quantity},
            version = version + 1,
            update_time = NOW()
        WHERE product_id = #{productId}
          AND available_stock >= #{quantity}
        """)
    int deductAvailableStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
