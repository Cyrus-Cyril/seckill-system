package com.example.seckill.controller;

import com.example.seckill.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Value("${instance.id:unknown}")
    private String instanceId;

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("instanceId", instanceId);
        data.put("time", LocalDateTime.now().toString());
        return ApiResponse.success(data);
    }
}