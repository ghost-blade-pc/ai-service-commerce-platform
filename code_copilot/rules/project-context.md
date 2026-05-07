---
alwaysApply: true
---
# 工程上下文

## 1. 应用概况
- 应用名: `s-pay-mall-ddd-lpc`
- 简介: 支付商城 DDD 项目，提供商品下单、支付宝支付、用户订单查询、退款、微信扫码登录，以及与拼团营销服务的锁单/结算交互。
- 技术栈: Java 17 / Spring Boot 2.7.12 / MyBatis / RabbitMQ / Retrofit2 / Guava EventBus / Alipay SDK / Weixin API
- 构建工具: Maven 多模块
- 根包名: `top.licodetech.mall`
- 当前主要启动模块: `s-pay-mall-ddd-lpc-app`

## 2. 目录结构与模块职责

| 模块 | 职责 | 典型路径 |
|------|------|----------|
| `s-pay-mall-ddd-lpc-api` | 对外服务契约、DTO、统一响应模型 | `api/IPayService.java`, `api/IAuthService.java`, `api/dto/*`, `api/response/Response.java` |
| `s-pay-mall-ddd-lpc-app` | Spring Boot 启动、配置装配、资源文件 | `Application.java`, `config/*`, `resources/application-*.yml` |
| `s-pay-mall-ddd-lpc-domain` | 领域模型、聚合、值对象、领域服务、领域端口接口 | `domain/order/*`, `domain/auth/*`, `domain/goods/*` |
| `s-pay-mall-ddd-lpc-trigger` | HTTP/MQ/Job 触发入口，协议转换，调用 API/领域服务 | `trigger/http/*`, `trigger/listener/*`, `trigger/job/*` |
| `s-pay-mall-ddd-lpc-infrastructure` | DAO、PO、Repository 实现、外部网关、事件发布、Redis 适配 | `infrastructure/adapter/*`, `infrastructure/gateway/*`, `infrastructure/dao/*` |
| `s-pay-mall-ddd-lpc-types` | 通用常量、异常、事件基类、SDK 工具 | `types/common/*`, `types/exception/*`, `types/event/*` |

## 3. DDD 分层架构

```text
trigger
  HTTP Controller / RabbitMQ Listener / Job
  负责协议接入、参数校验、日志、响应转换
        ↓
api
  Java 服务契约和 DTO，定义对外能力边界
        ↓
domain
  聚合、实体、值对象、领域服务、领域端口接口
  负责订单、支付、退款、拼团状态等核心业务规则
        ↓
infrastructure
  Repository 实现、MyBatis DAO、外部 HTTP 网关、MQ 发布
  负责技术细节和外部系统适配
        ↓
types
  通用异常、常量、事件基类、工具类
```

关键约束：

- `domain` 可以定义 `adapter/repository`、`adapter/port` 接口，但不能依赖 MyBatis、Retrofit、Spring MVC Controller。
- `infrastructure` 实现 `domain` 的端口和仓储接口，负责 PO/DTO 与领域对象转换。
- `trigger` 可以实现 `api` 接口，但业务规则应委托给 `domain` 服务。
- 跨服务 REST 调用优先放在 `infrastructure/gateway`，由 `infrastructure/adapter/port` 封装成领域可理解的端口。

## 4. 关键依赖

| 中间件 | 用途 | 备注 |
|--------|------|------|
| MySQL | 订单持久化 | MyBatis mapper 位于 `app/resources/mybatis/mapper` |
| RabbitMQ | 支付成功、拼团成团消息 | 配置位于 `application-*.yml` 的 `spring.rabbitmq.config` |
| 支付宝沙箱 | 创建支付单、接收支付回调 | `AliPayController#payNotify` 负责回调验签和改支付状态 |
| 微信 | 扫码登录、模板消息 | `LoginController`、`IWeixinApiService`、`WeixinLoginService` |
| 拼团营销服务 | 营销锁单、营销结算、成团通知 | `IGroupBuyMarketService` 通过 Retrofit 调用 |

## 5. 当前核心链路

- 创建支付单：`AliPayController#createPayOrder` → `IOrderService#createOrder` → `AbstractOrderService#createOrder` → `IProductPort` 查询商品 → `IOrderRepository#doSaveOrder` → 可选调用拼团营销锁单 → 支付宝预支付。
- 支付宝回调：`AliPayController#payNotify` → 支付宝验签 → `IOrderService#changeOrderPaySuccess` → 订单状态变更 → 发布支付成功事件。
- 拼团营销锁单：`AbstractOrderService#createOrder` → `lockMarketPayOrder` → `IGroupBuyMarketService#lockMarketPayOrder`。
- 拼团营销结算回调：`AliPayController#groupBuyNotify` → `IOrderService#changeOrderMarketSettlement` → 更新订单营销结算状态并发布支付成功事件。
- 用户订单查询：`AliPayController#queryUserOrderList` → `IOrderService#queryUserOrderList` → `IOrderRepository#queryUserOrderList`。
- 退款：`AliPayController#refundOrder` → `IOrderService#refundOrder` → 仓储更新订单退款状态。
