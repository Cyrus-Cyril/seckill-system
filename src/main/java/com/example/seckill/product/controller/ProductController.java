package com.example.seckill.product.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.product.entity.Product;
import com.example.seckill.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return ApiResponse.fail("商品不存在");
        }
        return ApiResponse.success(product);
    }
}
