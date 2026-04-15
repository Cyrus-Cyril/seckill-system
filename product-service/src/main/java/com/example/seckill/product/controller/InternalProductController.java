package com.example.seckill.product.controller;

import com.example.seckill.api.product.ProductSnapshot;
import com.example.seckill.product.entity.Product;
import com.example.seckill.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ProductSnapshot getSnapshot(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return null;
        }
        ProductSnapshot snapshot = new ProductSnapshot();
        snapshot.setId(product.getId());
        snapshot.setProductName(product.getProductName());
        snapshot.setPrice(product.getPrice());
        snapshot.setStatus(product.getStatus());
        return snapshot;
    }
}
