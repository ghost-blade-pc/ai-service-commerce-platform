# 未支付退单退款误触发与 MQ 重试风暴修复
> status: done
> created: 2026-05-08
> complexity: 高风险
> commits: `bbbb6b8 fix: avoid refunding unpaid orders`, `7f91a12 fix: add mq retry dlq and settlement idempotency`

## 1. 背景与目标

本次修复来自两个运行期问题：

1. 未支付退单被误认为需要资金退款。
   - 用户选择拼团营销后未支付，拼团侧发送 `topic.team_refund` 的 `type=unpaid_unlock`，支付商城仍调用 `IRefundPort#refund`。
   - 用户不选择拼团营销直接购买但未支付，退单也会进入原 `changeOrderRefundSuccess(orderId)` 语义，误触发退款端口。
2. 拼团达成后支付侧 MQ 消费疯狂刷日志。
   - 日志显示 `TeamSuccessTopicListener` 处理 `topic.team_success` 时 MyBatis 参数绑定失败。
   - RabbitMQ 默认异常重投导致同一消息被无限重试，形成日志风暴。

目标：

- 支付商城能识别拼团退单类型，并区分未支付解锁和已支付退款。
- 普通未支付订单退单只推进订单状态，不调用资金退款端口。
- 退款本地任务持久化 `refund_type`，补偿任务按类型处理，历史任务可从 message JSON 回补解析。
- `topic.team_success`、`topic.team_refund`、`topic.order_pay_success` 消费具备有限重试和 DLQ。
- 拼团结算回调具备幂等性，重复 `team_success` 不重复发布发货/支付成功事件。

## 2. Root Cause

### 2.1 未支付退单误触发退款

原逻辑中，支付商城消费 `topic.team_refund` 后只抽取 `orderId/outTradeNo`，最终进入 `OrderService#changeOrderRefundSuccess(String orderId)`。该方法默认认为已经发生过付款，必然计算退款金额并调用 `IRefundPort#refund(orderId, refundAmount)`。

该语义对已支付退款合理，但对以下场景错误：

- `unpaid_unlock`：拼团未支付未成团，只是解锁/退单完成，不存在资金退款。
- 普通直购未支付订单：用户未付款，退单应直接完成，不存在资金退款。

### 2.2 MQ 疯狂滚动

运行日志中的核心异常为：

```text
Parameter 'outTradeNoList' not found. Available parameters are [arg0, collection, list]
```

`pay_order_mapper.xml` 使用 `<foreach collection="outTradeNoList">`，但 `IOrderDao#changeOrderMarketSettlement(List<String> outTradeNoList)` 没有 `@Param("outTradeNoList")`。MyBatis 无法绑定该参数，`TeamSuccessTopicListener` 每次消费都失败并抛出异常。

RabbitMQ listener 默认异常后会 requeue，同一条消息没有有限重试和 DLQ，因此形成无限重试和日志刷屏。

## 3. 实现方案

### 3.1 退单类型建模

新增 `RefundTypeVO` 对齐拼团侧退单类型：

| 类型 | 语义 | 是否调用退款端口 |
|------|------|------------------|
| `unpaid_unlock` | 未支付未成团解锁 | 否 |
| `paid_unformed` | 已支付未成团退款 | 是 |
| `paid_formed` | 已支付已成团退款 | 是 |

`RefundSuccessTopicListener` 从 MQ message 解析 `type` 和 `outTradeNo/orderId`。`type` 缺失或非法时按永久异常记录并 ack，不进入本地退款任务，避免错误类型被补偿任务误退款。

### 3.2 领域退款完成语义

保留兼容方法：

```java
OrderEntity changeOrderRefundSuccess(String orderId);
```

该方法默认代表需要资金退款，兼容普通已支付退款和历史调用方。

新增类型化处理：

```java
OrderEntity changeOrderRefundSuccess(String orderId, RefundTypeVO refundType);
```

内部按 `refundType.isNeedPayRefund()` 决定是否调用 `IRefundPort#refund`。

普通直购未支付退单根据订单当前状态判断：

```java
needPayRefund = orderStatus != PAY_WAIT
```

因此 `PAY_WAIT` 未支付退单只推进订单状态到 `REFUNDED`，不调用退款端口。

### 3.3 本地退款任务扩展

`pay_refund_task` 新增 `refund_type varchar(32)`。

任务保存和补偿从只保存 `orderId` 改为保存：

- `orderId`
- `refundType`
- `message`

Job 查询待处理任务时返回 `RefundTaskEntity`，不再只返回订单号。历史任务如果 `refund_type` 为空，则尝试从 `message.type` 回补解析；仍无法解析则标记失败，避免误退款。

### 3.4 MQ 有限重试与 DLQ

新增 Spring Retry 支持：

- 最大尝试次数：3
- Backoff：1s、2x、最大 10s
- exhausted 后使用 `RejectAndDontRequeueRecoverer`，拒绝且不 requeue

为以下支付商城队列声明 DLX/DLQ：

- `s_pay_mall_queue_2_topic_team_success`
- `s_pay_mall_queue_2_topic_team_refund`
- `s_pay_mall_queue_2_order_pay_success`

DLQ 命名规则：

- DLX：`${queue}.dlx`
- DLQ：`${queue}.dlq`
- dead routing key：`${routingKey}.dead`

### 3.5 拼团结算幂等

将 `changeOrderMarketSettlement` 从批量 `IN` 更新改为按订单单条更新：

```sql
update pay_order
set status = 'MARKET', update_time = now()
where order_id = #{orderId}
  and status = 'PAY_SUCCESS'
```

仓储层只对 update count 为 1 的订单发布 `topic.order_pay_success`。重复 `team_success` 消息到达时，已是 `MARKET` 的订单不会重复发布后续事件。

## 4. 影响范围

- 资金：未支付订单不再触发模拟/真实退款端口。
- 订单状态：未支付退单最终仍使用现有 `REFUNDED` 表示退单完成。
- MQ：支付商城关键消费者从无限重投改为 3 次重试后进入 DLQ。
- 数据库：`pay_refund_task` 增加 `refund_type` 字段；已有环境需要手工执行 ALTER。
- 运维：已有 RabbitMQ 队列如果已经存在且没有 DLX 参数，部署前需删除并由应用重新声明，或手工重建带 DLX 的队列。

## 5. 运维 SQL

已有环境执行：

```sql
ALTER TABLE pay_refund_task
    ADD COLUMN refund_type varchar(32) DEFAULT NULL COMMENT '退单类型' AFTER order_id;
```

新建环境已同步到：

```text
docs/dev-ops/mysql/sql/s-pay-mall.sql
```

## 6. RabbitMQ 发布注意

RabbitMQ 不允许使用不同参数重复声明同名队列。若环境中已存在旧队列，应用启动时可能出现 `PRECONDITION_FAILED`。

部署前需要删除或重建以下旧队列：

```text
s_pay_mall_queue_2_topic_team_success
s_pay_mall_queue_2_topic_team_refund
s_pay_mall_queue_2_order_pay_success
```

应用启动后会重新声明主队列、DLX 和 DLQ。

## 7. 验收标准

- `unpaid_unlock` 消息完成订单退单状态，不调用 `IRefundPort#refund`。
- `paid_unformed` 和 `paid_formed` 消息完成订单退单状态，并调用 `IRefundPort#refund(orderId, refundAmount)`。
- 普通直购 `PAY_WAIT` 未支付退单不调用退款端口。
- `type` 缺失或非法时 listener 不抛给 MQ 容器，记录后 ack。
- 重复退款任务按 `orderId` 幂等，不重复退款。
- `team_success` 重复消息不会重复发布 `topic.order_pay_success`。
- MQ 消费异常最多重试 3 次，之后进入对应 DLQ。

