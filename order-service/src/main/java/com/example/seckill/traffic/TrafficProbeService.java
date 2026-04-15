package com.example.seckill.traffic;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.seckill.common.ApiResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TrafficProbeService {

    @SentinelResource(value = "hotspotQuery",
            blockHandlerClass = TrafficBlockHandlers.class,
            blockHandler = "handleHotspot")
    public ApiResponse<Map<String, Object>> hotspot(String userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }

    @SentinelResource(value = "unstableService",
            blockHandlerClass = TrafficBlockHandlers.class,
            blockHandler = "handleUnstable",
            fallbackClass = TrafficBlockHandlers.class,
            fallback = "fallbackUnstable")
    public ApiResponse<Map<String, Object>> unstable(String mode, long sleepMs) {
        if ("error".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("模拟异常, 用于触发降级");
        }
        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("线程被中断", e);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mode", mode);
        data.put("sleepMs", sleepMs);
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }
}
