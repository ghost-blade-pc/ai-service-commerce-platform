---
alwaysApply: false
description: "当涉及业务领域特定逻辑时应用本规则"
---
# 业务领域约束

## 1. 金额与时间

- 当前项目金额使用 `BigDecimal`，例如 `ProductEntity#price`、`OrderEntity#totalAmount`、`PayOrder#payAmount`。
- 新增金额字段必须明确业务含义：原价、实付、营销抵扣、退款金额，不允许混用。
- 金额计算使用 `BigDecimal`，禁止使用 `double` / `float`。
- 当前项目时间字段主要使用 `Date`，例如订单时间、支付时间；新增字段优先保持一致，除非有明确迁移方案。

## 2. 订单状态

- 订单状态统一通过 `OrderStatusVO` 表达，禁止在业务代码里散落字符串状态值。
- 新增状态必须补充：枚举定义、状态描述、是否可退款判断、数据库字段含义、状态流转规则。
- 涉及状态变更必须在 Spec 中列出：前置状态、目标状态、触发入口、幂等策略、失败处理。
- 支付成功、退款、关闭订单、拼团结算都属于高风险状态流转，必须人工复核。

## 3. 支付与退款

- 支付宝回调必须验签，不能绕过 `AlipaySignature` 校验。
- 支付回调和 MQ 消息消费必须具备幂等思路：重复通知不能重复发货、重复结算或重复退款。
- 退款接口必须校验用户、订单归属、订单状态和可退款条件，不能只按 `orderId` 更新。
- 日志中可以打印 `userId`、`orderId`、`productId`，但禁止打印密钥、私钥、完整 token、支付渠道敏感报文。

## 4. 拼团营销交互

- 支付商城调用拼团营销服务时，HTTP 网关接口放在 `infrastructure/gateway`，领域端口放在 `domain/order/adapter/port` 或已有合适端口。
- Retrofit DTO 放在 `infrastructure/gateway/dto`，不能直接泄漏到 `domain` 模型。
- 领域层只感知业务对象，例如 `MarketPayDiscountEntity`、`MarketTypeVO`、`ShopCartEntity`。
- 拼团锁单、营销结算、成团通知都必须在 Spec 中明确调用方向、接口路径、请求/响应字段和失败策略。
- 配置优先复用 `app.config.group-buy-market.*`，不要硬编码服务地址、渠道值、回调地址。

## 5. 领域模型边界

- `domain/model/entity` 放领域实体，表达业务状态和行为。
- `domain/model/aggregate` 放跨实体一致性构造或聚合根相关逻辑。
- `domain/model/valobj` 放不可变或枚举型值对象，例如 `MarketTypeVO`、`OrderStatusVO`。
- `domain/service` 放领域服务和编排，不直接依赖 Controller、MyBatis Mapper、Retrofit `Call`。
- `domain/adapter/repository` 和 `domain/adapter/port` 只定义接口，不写技术实现。
