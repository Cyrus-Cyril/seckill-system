package com.example.seckill.product.mapper;

import com.example.seckill.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper {

    @Select("""
        SELECT id, product_name, price, status, create_time, update_time
        FROM tb_product
        WHERE id = #{id}
        LIMIT 1
        """)
    Product findById(Long id);
}