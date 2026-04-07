package com.example.seckill.order.dto;

public class SeckillSubmitResponse {

    private Long orderId;
    private String status;
    private String message;

    public SeckillSubmitResponse() {
    }

    public SeckillSubmitResponse(Long orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
