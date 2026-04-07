package com.example.seckill.order.mapper;

import com.example.seckill.order.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
        INSERT INTO tb_order(id, order_no, user_id, product_id, quantity, order_amount, status, order_type)
        VALUES(#{id}, #{orderNo}, #{userId}, #{productId}, #{quantity}, #{orderAmount}, #{status}, #{orderType})
        """)
    int insert(Order order);

    @Select("""
        SELECT id, order_no, user_id, product_id, quantity, order_amount, status, order_type, create_time, update_time
        FROM tb_order
        WHERE id = #{id}
        LIMIT 1
        """)
    Order findById(Long id);

    @Select("""
        SELECT id, order_no, user_id, product_id, quantity, order_amount, status, order_type, create_time, update_time
        FROM tb_order
        WHERE order_no = #{orderNo}
        LIMIT 1
        """)
    Order findByOrderNo(String orderNo);

    @Select("""
        SELECT id, order_no, user_id, product_id, quantity, order_amount, status, order_type, create_time, update_time
        FROM tb_order
        WHERE user_id = #{userId}
        ORDER BY create_time DESC
        """)
    List<Order> findByUserId(Long userId);
}
