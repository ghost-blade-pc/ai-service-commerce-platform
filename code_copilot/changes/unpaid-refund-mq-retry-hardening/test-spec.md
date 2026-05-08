# 测试 Spec — 未支付退单退款误触发与 MQ 重试风暴修复
> status: done
> created: 2026-05-08

## 1. 测试目标

- 验证未支付拼团退单和未支付普通直购退单不会调用退款端口。
- 验证已支付退单仍调用退款端口。
- 验证退款 MQ 非法类型不会抛给 RabbitMQ 容器。
- 验证退款任务补偿能携带 `refund_type`。
- 验证拼团结算重复消息不会重复发布 `topic.order_pay_success`。
- 验证 RabbitMQ 队列声明包含 DLX/DLQ，listener 有有限重试配置。

## 2. 覆盖场景

### P0 — 退款资金安全

| 测试类 | 场景 | 预期 |
|--------|------|------|
| `OrderServiceRefundTest` | `unpaid_unlock` 拼团退单 | 订单变为 `REFUNDED`，`IRefundPort#refund` 不调用 |
| `OrderServiceRefundTest` | 普通 `PAY_WAIT` 订单退单 | 订单变为 `REFUNDED`，`IRefundPort#refund` 不调用 |
| `OrderServiceRefundTest` | `paid_unformed` 退单 | 订单变为 `REFUNDED`，调用退款端口 |
| `OrderServiceRefundTest` | `paid_formed` 退单 | 订单变为 `REFUNDED`，调用退款端口 |
| `OrderServiceRefundTest` | 重复 `REFUNDED` 消息 | 幂等返回，不重复退款 |

### P1 — Listener 和任务补偿

| 测试类 | 场景 | 预期 |
|--------|------|------|
| `RefundSuccessTopicListenerTest` | 消息缺少 `type` | listener 记录后返回，不抛异常 |
| `RefundSuccessTopicListenerTest` | 非法 `type` | listener 记录后返回，不抛异常 |
| `RefundSuccessTopicListenerTest` | 合法 `type` | 调用 `receiveRefundSuccessMessage(orderId, refundType, message)` |
| `OrderServiceRefundTest` | `refund_type` 为空但 message 有 type | 从 message 回补解析 |
| `OrderServiceRefundTest` | `refund_type` 和 message 都缺失 | 任务标记失败，避免误退款 |

### P1 — MQ 重试/DLQ 与结算幂等

| 测试类 | 场景 | 预期 |
|--------|------|------|
| `RabbitMQConfigTest` | 主队列参数 | 包含 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` |
| `RabbitMQConfigTest` | retry interceptor bean | bean 可创建，供 listener container factory 使用 |
| `OrderRepositoryMarketSettlementTest` | 两个订单中一个更新成功、一个已处理 | 只对更新成功订单发布 `topic.order_pay_success` |
| `OrderRepositoryMarketSettlementTest` | 空列表/null | 不调用 DAO，不发布 MQ |

## 3. 验证命令

```bash
mvn -pl s-pay-mall-ddd-lpc-app -am \
  -Dtest=OrderRepositoryMarketSettlementTest,RabbitMQConfigTest,OrderServiceRefundTest,RefundSuccessTopicListenerTest \
  -DfailIfNoTests=false \
  -DskipTests=false \
  test
```

## 4. 验证结果

```text
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 5. 未覆盖但需联调关注

- RabbitMQ 真实 DLQ 入队行为需在联调环境通过故意抛异常验证。
- 旧队列迁移需在部署窗口确认，避免 `PRECONDITION_FAILED` 导致应用启动失败。
- 真实支付宝退款接入后，需要补充真实退款失败、超时、重复退款防护测试。

