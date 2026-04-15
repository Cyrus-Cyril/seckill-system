# Nacos + Gateway + Sentinel + 微服务拆分验证说明

## 1. 启动基础环境
在项目根目录执行：

```powershell
docker compose up -d nacos mysql redis kafka sentinel-dashboard user-service product-service order-service-1 order-service-2 gateway
```

服务端口：
- Nacos 控制台：`http://localhost:8848/nacos`
- Sentinel Dashboard：`http://localhost:8858`
- 服务网关：`http://localhost:8088`
- 用户服务：`http://localhost:8081`
- 商品库存服务：`http://localhost:8082`
- 订单服务两个实例：`http://localhost:8083`、`http://localhost:8084`

## 2. 在 Nacos 中准备动态配置
在 Nacos 新建配置：
- Data ID: `shared-seckill.properties`
- Group: `DEFAULT_GROUP`

示例内容：

```properties
seckill.demo.message=hello-from-nacos
seckill.demo.gateway-prefix=/api
seckill.sentinel.hotspot-qps=2
seckill.sentinel.order-qps=5
seckill.sentinel.degrade-threshold-ms=300
```

保存后，重新访问以下接口确认配置生效：
- 直连订单服务：`GET http://localhost:8083/api/config/dynamic`
- 经过网关：`GET http://localhost:8088/api/config/dynamic`

修改 `seckill.demo.message` 后再次请求 `/api/test/ping` 或 `/api/config/dynamic`，可验证动态刷新。

## 3. 测试注册发现与动态路由
先确认 `user-service`、`product-service`、`order-service` 都已在 Nacos 中注册，其中 `order-service` 会有两个实例。然后通过网关调用：

```powershell
curl http://localhost:8088/api/test/ping
curl http://localhost:8088/api/test/ping
```

返回中的 `instanceId` 会在 `order1` 和 `order2` 之间切换，表示 Gateway 通过 `lb://order-service` 完成服务发现和负载均衡。

还可以分别验证多服务路由：

```powershell
curl http://localhost:8088/api/users/check?username=test
curl http://localhost:8088/api/products/1
curl http://localhost:8088/api/orders/1
```

## 4. 测试流量治理
### 热点参数限流
接口：`GET /api/test/hotspot?userId=guest`

```powershell
for ($i=0; $i -lt 5; $i++) { curl "http://localhost:8088/api/test/hotspot?userId=guest" }
```

超过阈值后会返回“请求被限流/熔断”。

### 慢调用降级 / 熔断
接口：`GET /api/test/unstable?mode=slow&sleepMs=600`

连续高频访问后，Sentinel 会根据 `seckill.sentinel.degrade-threshold-ms` 触发降级。

### 秒杀下单接口限流
接口：`POST /api/orders/seckill`

该接口通过 Sentinel 资源 `submitSeckillOrder` 配置 QPS 限流，压测时会出现被限流的响应。

## 5. JMeter 压测建议
推荐建立 3 个线程组：
- 线程组 A：压测 `/api/test/hotspot?userId=guest`，50 线程，Ramp-Up 5 秒，循环 20 次。
- 线程组 B：压测 `/api/test/unstable?mode=slow&sleepMs=600`，20 线程，Ramp-Up 3 秒，循环 10 次。
- 线程组 C：压测 `/api/orders/seckill`，按你现有请求体造数，观察正常响应和限流响应占比。

重点观察：
- 平均响应时间是否在触发慢调用降级后下降。
- 错误率是否转换为可控的限流/降级响应，而不是服务雪崩。
- Sentinel Dashboard 中的簇点链路与实时监控。

## 6. 本次代码入口
- 父工程与模块：`pom.xml`
- 公共模块：`common-core/`
- 用户服务：`user-service/`
- 商品库存服务：`product-service/`
- 订单服务：`order-service/`
- 网关路由：`gateway/src/main/resources/application.yml`
