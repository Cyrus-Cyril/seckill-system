package com.example.seckill.order.client;

import com.example.seckill.api.product.ProductSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductRemoteClient {

    @GetMapping("/{id}")
    ProductSnapshot getProduct(@PathVariable("id") Long id);
}
