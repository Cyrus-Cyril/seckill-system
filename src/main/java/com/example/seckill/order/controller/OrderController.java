package com.example.seckill.order.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.order.dto.PaymentSubmitResponse;
import com.example.seckill.order.dto.SeckillOrderRequest;
import com.example.seckill.order.dto.SeckillSubmitResponse;
import com.example.seckill.order.entity.Order;
import com.example.seckill.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/seckill")
    public ApiResponse<SeckillSubmitResponse> submitSeckillOrder(@Valid @RequestBody SeckillOrderRequest request) {
        return ApiResponse.success(orderService.submitSeckillOrder(request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<PaymentSubmitResponse> payOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.payOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Order> getById(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderById(orderId));
    }

    @GetMapping
    public ApiResponse<List<Order>> getByUserId(@RequestParam Long userId) {
        return ApiResponse.success(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/search")
    public ApiResponse<Order> getByOrderNo(@RequestParam String orderNo) {
        return ApiResponse.success(orderService.getOrderByOrderNo(orderNo));
    }
}
