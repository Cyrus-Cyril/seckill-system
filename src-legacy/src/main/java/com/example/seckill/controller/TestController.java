package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.traffic.TrafficProbeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TrafficProbeService trafficProbeService;

    @Value("${instance.id:unknown}")
    private String instanceId;

    @Value("${seckill.demo.message:hello-from-default}")
    private String dynamicMessage;

    public TestController(TrafficProbeService trafficProbeService) {
        this.trafficProbeService = trafficProbeService;
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("instanceId", instanceId);
        data.put("dynamicMessage", dynamicMessage);
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }

    @GetMapping("/hotspot")
    public ApiResponse<Map<String, Object>> hotspot(@RequestParam(defaultValue = "guest") String userId) {
        return trafficProbeService.hotspot(userId);
    }

    @GetMapping("/unstable")
    public ApiResponse<Map<String, Object>> unstable(@RequestParam(defaultValue = "ok") String mode,
                                                     @RequestParam(defaultValue = "0") long sleepMs) {
        return trafficProbeService.unstable(mode, sleepMs);
    }
}
