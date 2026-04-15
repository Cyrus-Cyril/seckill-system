package com.example.seckill.traffic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seckill.sentinel")
public class TrafficRuleProperties {

    private double hotspotQps = 2;
    private double orderQps = 5;
    private int degradeThresholdMs = 300;

    public double getHotspotQps() {
        return hotspotQps;
    }

    public void setHotspotQps(double hotspotQps) {
        this.hotspotQps = hotspotQps;
    }

    public double getOrderQps() {
        return orderQps;
    }

    public void setOrderQps(double orderQps) {
        this.orderQps = orderQps;
    }

    public int getDegradeThresholdMs() {
        return degradeThresholdMs;
    }

    public void setDegradeThresholdMs(int degradeThresholdMs) {
        this.degradeThresholdMs = degradeThresholdMs;
    }
}
