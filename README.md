# 秒杀系统微服务课程作业

这是一个面向分布式系统课程作业的秒杀系统实验项目。项目从原始单体结构演进为一个基于 Spring Cloud Alibaba 的微服务系统，围绕用户、商品库存、订单三个核心业务服务展开，并接入了 Nacos、Spring Cloud Gateway、Redis、Kafka 和 Sentinel，用于演示服务注册发现、配置中心、网关路由、异步削峰和流量治理等分布式基础能力。

## 项目目标

- 将原始业务模块拆分为独立微服务，形成更符合课程要求的分布式系统结构
- 使用 Nacos 完成服务注册发现与动态配置管理
- 使用 Spring Cloud Gateway 作为统一入口
- 使用 Redis 支撑秒杀库存预扣与缓存
- 使用 Kafka 完成异步下单、库存扣减、支付结果通知
- 使用 Sentinel 演示限流、降级和熔断

## 当前架构

项目目前包含以下模块：

- `common-core`
  公共模块，包含通用响应体、异常、配置类、雪花 ID 和跨服务共享 DTO
- `user-service`
  用户服务，负责注册、登录、用户信息查询
- `product-service`
  商品与库存服务，负责商品查询、库存缓存预热、库存扣减消息消费
- `order-service`
  订单服务，负责秒杀下单、订单查询、支付消息处理、流量治理演示
- `gateway`
  网关服务，负责统一入口与基于服务名的动态路由

系统中的基础设施包括：

- Nacos：服务注册与配置中心
- Gateway：统一网关入口
- MySQL：业务数据存储
- Redis：缓存与库存预扣
- Kafka：异步消息总线
- Sentinel：限流、熔断、降级

## 项目结构

```text
seckill-system/
├── common-core/         # 公共模块
├── user-service/        # 用户服务
├── product-service/     # 商品与库存服务
├── order-service/       # 订单服务
├── gateway/             # 网关服务
├── docs/                # 实验说明与文档
├── sql/                 # 初始化脚本
├── docker-compose.yml   # 本地联调环境
├── pom.xml              # Maven 父工程
└── src-legacy/          # 保留的旧单体代码，不再作为当前源码入口
```

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| 构建 | Maven |
| 微服务 | Spring Boot 3.2, Spring Cloud 2023, Spring Cloud Alibaba |
| 注册配置 | Nacos |
| 网关 | Spring Cloud Gateway |
| 持久层 | MyBatis + MySQL |
| 缓存 | Redis |
| 消息队列 | Kafka |
| 流量治理 | Sentinel |
| 容器编排 | Docker Compose |

## 服务划分说明

### 1. 用户服务 `user-service`

提供接口：

- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/users/{id}`
- `GET /api/users/check`

### 2. 商品库存服务 `product-service`

提供接口：

- `GET /api/products/{id}`
- `GET /internal/products/{id}`

其中 `/internal/products/{id}` 供订单服务进行内部远程调用，用于演示服务间通信。

### 3. 订单服务 `order-service`

提供接口：

- `POST /api/orders/seckill`
- `POST /api/orders/{orderId}/pay`
- `GET /api/orders/{orderId}`
- `GET /api/orders`
- `GET /api/orders/search`
- `GET /api/test/ping`
- `GET /api/test/hotspot`
- `GET /api/test/unstable`
- `GET /api/config/dynamic`

### 4. 网关 `gateway`

网关统一暴露：

- `/api/users/**`
- `/api/products/**`
- `/api/orders/**`
- `/api/test/**`
- `/api/config/**`

## 分布式特性

这个项目当前已经具备较完整的课程作业型分布式特征：

- 业务拆分：用户、商品库存、订单拆为独立服务
- 服务注册发现：各服务注册到 Nacos
- 配置中心：共享配置从 Nacos 动态下发
- 网关路由：通过 Gateway 按路径转发到不同服务
- 服务间调用：订单服务通过 Feign 调用商品服务
- 多实例部署：`order-service` 支持双实例运行
- 异步消息：订单创建、库存扣减、支付结果通过 Kafka 串联
- 缓存与库存预扣：Redis 作为秒杀场景支撑
- 流量治理：Sentinel 用于热点限流、慢调用降级、下单限流

## 快速启动

### 1. 启动基础环境

在项目根目录执行：

```powershell
docker compose up -d nacos mysql redis kafka sentinel-dashboard user-service product-service order-service-1 order-service-2 gateway
```

### 2. 访问端口

- Nacos 控制台：`http://localhost:8848/nacos`
- Sentinel Dashboard：`http://localhost:8858`
- 网关：`http://localhost:8088`
- 用户服务：`http://localhost:8081`
- 商品库存服务：`http://localhost:8082`
- 订单服务实例 1：`http://localhost:8083`
- 订单服务实例 2：`http://localhost:8084`

## Nacos 配置示例

在 Nacos 中新增配置：

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

修改保存后，可通过以下接口验证动态刷新：

- `GET http://localhost:8083/api/config/dynamic`
- `GET http://localhost:8088/api/config/dynamic`

## 验证建议

### 1. 服务注册与动态路由

通过网关访问：

```powershell
curl http://localhost:8088/api/test/ping
curl http://localhost:8088/api/test/ping
```

如果返回中的 `instanceId` 在 `order1` 与 `order2` 之间切换，说明订单服务的双实例负载均衡正常。

### 2. 多服务网关转发

```powershell
curl http://localhost:8088/api/users/check?username=test
curl http://localhost:8088/api/products/1
curl http://localhost:8088/api/orders/1
```

### 3. 流量治理

热点参数限流：

```powershell
for ($i=0; $i -lt 5; $i++) { curl "http://localhost:8088/api/test/hotspot?userId=guest" }
```

慢调用降级：

```powershell
curl "http://localhost:8088/api/test/unstable?mode=slow&sleepMs=600"
```

下单限流：

```powershell
curl -Method POST "http://localhost:8088/api/orders/seckill"
```

## JMeter 压测建议

推荐建立以下线程组：

- 线程组 A：压测 `/api/test/hotspot?userId=guest`
- 线程组 B：压测 `/api/test/unstable?mode=slow&sleepMs=600`
- 线程组 C：压测 `/api/orders/seckill`

建议重点观察：

- 平均响应时间变化
- 限流与降级返回比例
- Sentinel Dashboard 中的实时监控

## Maven 构建

在项目根目录执行：

```powershell
.\mvnw.cmd -DskipTests package
```

当前工程已经调整为 Maven 多模块结构，构建会同时打包：

- `common-core`
- `user-service`
- `product-service`
- `order-service`
- `gateway`

## 文档

- 微服务联调与验证说明：[docs/nacos-gateway-sentinel.md](docs/nacos-gateway-sentinel.md)

## 说明

- `src-legacy/` 为保留的旧单体代码，用于对照演进过程，不再作为当前微服务版本的主源码目录
- 如果 IDE 出现旧代码干扰，请确保以根目录 `pom.xml` 作为 Maven 父工程重新加载项目
