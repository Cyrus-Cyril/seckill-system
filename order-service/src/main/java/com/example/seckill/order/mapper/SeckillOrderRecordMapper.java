package com.example.seckill.order.mapper;

import com.example.seckill.order.entity.SeckillOrderRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SeckillOrderRecordMapper {

    @Insert("""
        INSERT INTO tb_seckill_order_record(user_id, product_id, order_id)
        VALUES(#{userId}, #{productId}, #{orderId})
        """)
    int insert(SeckillOrderRecord record);

    @Select("""
        SELECT id, user_id, product_id, order_id, create_time
        FROM tb_seckill_order_record
        WHERE user_id = #{userId} AND product_id = #{productId}
        LIMIT 1
        """)
    SeckillOrderRecord findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
