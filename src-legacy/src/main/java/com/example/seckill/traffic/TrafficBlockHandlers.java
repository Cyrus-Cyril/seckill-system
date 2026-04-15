package com.example.seckill.traffic;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.seckill.common.ApiResponse;
import com.example.seckill.order.dto.SeckillSubmitResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TrafficBlockHandlers {

    private TrafficBlockHandlers() {
    }

    public static ApiResponse<Map<String, Object>> handleHotspot(String userId, BlockException ex) {
        return blocked("hotspot-limited", userId, ex);
    }

    public static ApiResponse<SeckillSubmitResponse> handleSeckill(Long userId, Long productId, BlockException ex) {
        return ApiResponse.fail("请求被限流/熔断: order-flow-limited " + userId + ":" + productId + ", rule=" + ex.getRule());
    }

    public static ApiResponse<Map<String, Object>> handleUnstable(String mode, long sleepMs, BlockException ex) {
        return blocked("degrade-or-circuit-open", mode + ":" + sleepMs, ex);
    }

    public static ApiResponse<Map<String, Object>> fallbackUnstable(String mode, long sleepMs, Throwable ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", mode);
        data.put("sleepMs", sleepMs);
        data.put("fallback", true);
        data.put("reason", ex.getMessage());
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.fail("服务降级触发: " + data);
    }

    private static ApiResponse<Map<String, Object>> blocked(String scene, String value, BlockException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scene", scene);
        data.put("value", value);
        data.put("rule", ex.getRule());
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.fail("请求被限流/熔断: " + data);
    }
}
