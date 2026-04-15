package com.example.seckill.order.entity;

public final class OrderStatus {

    public static final int PENDING_STOCK = 0;
    public static final int PENDING_PAYMENT = 1;
    public static final int PAID = 2;
    public static final int FAILED = 3;

    private OrderStatus() {
    }
}
