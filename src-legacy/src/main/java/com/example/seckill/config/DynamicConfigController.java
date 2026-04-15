package com.example.seckill.config;

import com.example.seckill.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RefreshScope
@RestController
@RequestMapping("/api/config")
public class DynamicConfigController {

    @Value("${seckill.demo.message:hello-from-default}")
    private String message;

    @Value("${seckill.demo.gateway-prefix:/seckill}")
    private String gatewayPrefix;

    @GetMapping("/dynamic")
    public ApiResponse<Map<String, Object>> readDynamicConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        data.put("gatewayPrefix", gatewayPrefix);
        return ApiResponse.success(data);
    }
}
