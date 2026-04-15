package com.example.seckill.traffic;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(TrafficRuleProperties.class)
public class SentinelRuleConfiguration {

    private final TrafficRuleProperties properties;

    public SentinelRuleConfiguration(TrafficRuleProperties properties) {
        this.properties = properties;
        loadRules();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void reloadOnConfigChange(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(key -> key.startsWith("seckill.sentinel."))) {
            loadRules();
        }
    }

    private void loadRules() {
        FlowRule orderFlow = new FlowRule();
        orderFlow.setResource("submitSeckillOrder");
        orderFlow.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderFlow.setCount(properties.getOrderQps());

        DegradeRule unstableDegrade = new DegradeRule();
        unstableDegrade.setResource("unstableService");
        unstableDegrade.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        unstableDegrade.setCount(properties.getDegradeThresholdMs());
        unstableDegrade.setMinRequestAmount(3);
        unstableDegrade.setTimeWindow(15);
        unstableDegrade.setStatIntervalMs(30000);
        unstableDegrade.setSlowRatioThreshold(0.4);

        ParamFlowRule hotspotRule = new ParamFlowRule("hotspotQuery")
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(properties.getHotspotQps())
                .setDurationInSec(1);
        hotspotRule.setParamFlowItemList(List.of(
                new ParamFlowItem().setObject("vip").setClassType(String.class.getName()).setCount(1)
        ));

        FlowRuleManager.loadRules(List.of(orderFlow));
        DegradeRuleManager.loadRules(List.of(unstableDegrade));
        ParamFlowRuleManager.loadRules(List.of(hotspotRule));
    }
}
